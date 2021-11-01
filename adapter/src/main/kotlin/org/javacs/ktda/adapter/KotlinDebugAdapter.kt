package org.javacs.ktda.adapter

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.io.InputStream
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ThreadLocalRandom
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.javacs.kt.LOG
import org.javacs.kt.LogLevel
import org.javacs.kt.LogMessage
import org.javacs.kt.util.AsyncExecutor
import org.javacs.ktda.util.JSON_LOG
import org.javacs.ktda.util.KotlinDAException
import org.javacs.ktda.util.ObjectPool
import org.javacs.ktda.util.waitFor
import org.javacs.ktda.core.Debuggee
import org.javacs.ktda.core.DebugContext
import org.javacs.ktda.core.exception.DebuggeeException
import org.javacs.ktda.core.event.DebuggeeEventBus
import org.javacs.ktda.core.event.BreakpointStopEvent
import org.javacs.ktda.core.event.StepStopEvent
import org.javacs.ktda.core.stack.StackFrame
import org.javacs.ktda.core.launch.DebugLauncher
import org.javacs.ktda.core.launch.LaunchConfiguration
import org.javacs.ktda.core.launch.AttachConfiguration
import org.javacs.ktda.core.breakpoint.ExceptionBreakpoint
import org.javacs.ktda.classpath.debugClassPathResolver
import org.javacs.ktda.classpath.findValidKtFilePath

/** The debug server interface conforming to the Debug Adapter Protocol */
class KotlinDebugAdapter(
	private val launcher: DebugLauncher
) : IDebugProtocolServer {
	private val async = AsyncExecutor()
	private val launcherAsync = AsyncExecutor()
	private val stdoutAsync = AsyncExecutor()
	private val stderrAsync = AsyncExecutor()
	
	private var debuggee: Debuggee? = null
	private var client: IDebugProtocolClient? = null
	private var converter = DAPConverter()
	private val context = DebugContext()

	private val exceptionsPool = ObjectPool<Long, DebuggeeException>() // Contains exceptions thrown by the debuggee owned by thread ids
	
	// TODO: This is a workaround for https://github.com/eclipse/lsp4j/issues/229
	// For more information, see launch() method
	private var configurationDoneResponse: CompletableFuture<Void>? = null
	
	override fun initialize(args: InitializeRequestArguments): CompletableFuture<Capabilities> = async.compute {
		converter.lineConverter = LineNumberConverter(
			externalLineOffset = if (args.linesStartAt1) 0 else -1
		)
		converter.columnConverter = LineNumberConverter(
			externalLineOffset = if (args.columnsStartAt1) 0 else -1
		)
		
		val capabilities = Capabilities()
		capabilities.supportsConfigurationDoneRequest = true
		capabilities.supportsCompletionsRequest = true
		capabilities.supportsExceptionInfoRequest = true
		capabilities.exceptionBreakpointFilters = ExceptionBreakpoint.values()
			.map(converter::toDAPExceptionBreakpointsFilter)
			.toTypedArray()
		
		LOG.trace("Returning capabilities...")
		capabilities
	}
	
	fun connect(client: IDebugProtocolClient) {
		connectLoggingBackend(client)
		this.client = client
		client.initialized()
		LOG.info("Connected to client")
	}
	
	override fun runInTerminal(args: RunInTerminalRequestArguments): CompletableFuture<RunInTerminalResponse> = notImplementedDAPMethod()
	
	override fun configurationDone(args: ConfigurationDoneArguments?): CompletableFuture<Void> {
		LOG.trace("Got configurationDone request")
		val response = CompletableFuture<Void>()
		configurationDoneResponse = response
		return response
	}

	override fun launch(args: Map<String, Any>) = launcherAsync.execute {
		performInitialization()

		LOG.debug("launch args: $args")

		val projectRoot = (args["projectRoot"] as? String)?.let { Paths.get(it) }
			?: throw missingRequestArgument("launch", "projectRoot")

		val mainClass = (args["mainClass"] as? String)
			?: throw missingRequestArgument("launch", "mainClass")

		val vmArguments = (args["vmArguments"] as? String) ?: ""

		var cwd = (args["cwd"] as? String).let { if(it.isNullOrBlank()) projectRoot else Paths.get(it) }

		// Cast from com.google.gson.internal.LinkedTreeMap
		@Suppress("UNCHECKED_CAST")
		var envs = args["envs"] as? Map<String, String> ?: mapOf()

		setupCommonInitializationParams(args)

		val config = LaunchConfiguration(
			debugClassPathResolver(listOf(projectRoot)).classpathOrEmpty,
			mainClass,
			projectRoot,
			vmArguments,
			cwd,
			envs
		)
		debuggee = launcher.launch(
			config,
			context
		).also(::setupDebuggeeListeners)
		LOG.trace("Instantiated debuggee")
	}
	
	private fun missingRequestArgument(requestName: String, argumentName: String) =
		KotlinDAException("Sent $requestName to debug adapter without the required argument'$argumentName'")
	
	private fun performInitialization() {
		client!!.initialized()
		
		// Wait for configurationDone response to fully return
		// as sketched in https://github.com/Microsoft/vscode/issues/4902#issuecomment-368583522
		// TODO: Find a cleaner solution once https://github.com/eclipse/lsp4j/issues/229 is resolved
		// (LSP4J does currently not provide a mechanism to hook into the request/response machinery)
		
		LOG.trace("Waiting for configurationDoneResponse")
		waitFor("configuration done response") { (configurationDoneResponse?.numberOfDependents ?: 0) != 0 }
		LOG.trace("Done waiting for configurationDoneResponse")
	}
	
	private fun setupDebuggeeListeners(debuggee: Debuggee) {
		val eventBus = debuggee.eventBus
		eventBus.exitListeners.add {
			// TODO: Use actual exitCode instead
			sendExitEvent(0)
		}
		eventBus.breakpointListeners.add {
			sendStopEvent(it.threadID, StoppedEventArgumentsReason.BREAKPOINT)
		}
		eventBus.stepListeners.add {
			sendStopEvent(it.threadID, StoppedEventArgumentsReason.STEP)
		}
		eventBus.exceptionListeners.add {
			exceptionsPool.store(it.threadID, it.exception)
			sendStopEvent(it.threadID, StoppedEventArgumentsReason.EXCEPTION)
		}
		eventBus.threadListeners.add {
			sendThreadEvent(it.threadID, converter.toDAPThreadEventReason(it.reason))
		}
		stdoutAsync.execute {
			debuggee.stdout?.let { pipeStreamToOutput(it, OutputEventArgumentsCategory.STDOUT) }
		}
		stderrAsync.execute {
			debuggee.stderr?.let { pipeStreamToOutput(it, OutputEventArgumentsCategory.STDERR) }
		}
		LOG.trace("Configured debuggee listeners")
	}
	
	private fun pipeStreamToOutput(stream: InputStream, outputCategory: String) {
		stream.bufferedReader().use {
			var line = it.readLine()
			while (line != null) {
				client?.output(OutputEventArguments().apply {
					category = outputCategory
					output = line + System.lineSeparator()
				})
				line = it.readLine()
			}
		}
	}

	private fun sendThreadEvent(threadId: Long, reason: String) {
		client!!.thread(ThreadEventArguments().also {
			it.reason = reason
			it.threadId = threadId.toInt()
		})
	}
	
	private fun sendStopEvent(threadId: Long, reason: String) {
		client!!.stopped(StoppedEventArguments().also {
			it.reason = reason
			it.threadId = threadId.toInt()
		})
	}
	
	private fun sendExitEvent(exitCode: Long) {
		client!!.exited(ExitedEventArguments().also {
			it.exitCode = exitCode.toInt()
		})
		client!!.terminated(TerminatedEventArguments())
		LOG.info("Sent exit event")
	}

	override fun attach(args: Map<String, Any>) = launcherAsync.execute {
		performInitialization()
		
		val projectRoot = (args["projectRoot"] as? String)?.let { Paths.get(it) }
			?: throw missingRequestArgument("attach", "projectRoot")
		
		val hostName = (args["hostName"] as? String)
			?: throw missingRequestArgument("attach", "hostName")
		
		val port = (args["port"] as? Double)?.toInt()
			?: throw missingRequestArgument("attach", "port")
		
		val timeout = (args["timeout"] as? Double)?.toInt()
			?: throw missingRequestArgument("attach", "timeout")
		
		setupCommonInitializationParams(args)
		
		debuggee = launcher.attach(
			AttachConfiguration(projectRoot, hostName, port, timeout),
			context
		).also(::setupDebuggeeListeners)

		// Since we are attaching to a running VM, we have to send custom
		// 'start' events for all executing threads
		for (thread in debuggee!!.threads) {
			sendThreadEvent(thread.id, ThreadEventArgumentsReason.STARTED)
		}
	}
	
	private fun setupCommonInitializationParams(args: Map<String, Any>) {
		val logLevel = (args["logLevel"] as? String)?.let(LogLevel::valueOf)
			?: LogLevel.INFO
		
		LOG.level = logLevel

		connectJsonLoggingBackend(args)
	}
	
	private fun connectJsonLoggingBackend(args: Map<String, Any>) {
		val enableJsonLogging = (args["enableJsonLogging"] as? Boolean) ?: false
		
		if (enableJsonLogging) {
			val jsonLogFile = (args["jsonLogFile"] as? String)?.let(::File)
				?: throw missingRequestArgument("launch/attach", "jsonLogFile")
			val newline = System.lineSeparator()
			
			if (!jsonLogFile.exists()) {
				jsonLogFile.createNewFile()
			}
			
			JSON_LOG.connectOutputBackend { msg -> jsonLogFile.appendText("[${msg.level}] ${msg.message}$newline") }
			JSON_LOG.connectErrorBackend { msg -> jsonLogFile.appendText("Error: [${msg.level}] ${msg.message}$newline") }
		}
	}
	
	override fun restart(args: RestartArguments): CompletableFuture<Void> = notImplementedDAPMethod()
	
	override fun disconnect(args: DisconnectArguments) = async.execute {
		debuggee?.exit()
	}
	
	override fun setBreakpoints(args: SetBreakpointsArguments) = async.compute {
		LOG.debug("{} breakpoints found", args.breakpoints.size)
		
		// TODO: Support logpoints and conditional breakpoints
		
		val placedBreakpoints = context
			.breakpointManager
			.setAllIn(
				converter.toInternalSource(args.source),
				args.breakpoints.map { converter.toInternalSourceBreakpoint(args.source, it) }
			)
			.map(converter::toDAPBreakpoint)
			.toTypedArray()
		
		SetBreakpointsResponse().apply {
			breakpoints = placedBreakpoints
		}
	}
	
	override fun setFunctionBreakpoints(args: SetFunctionBreakpointsArguments): CompletableFuture<SetFunctionBreakpointsResponse> = notImplementedDAPMethod()
	
	override fun setExceptionBreakpoints(args: SetExceptionBreakpointsArguments) = async.execute {
		args.filters
			.map(converter::toInternalExceptionBreakpoint)
			.toSet()
			.let(context.breakpointManager.exceptionBreakpoints::setAll)
	}
	
	override fun continue_(args: ContinueArguments) = async.compute {
		var success = debuggee!!.threadByID(args.threadId.toLong())?.resume() ?: false
		var allThreads = false

		if (!success) {
			debuggee!!.resume()
			success = true
			allThreads = true
		}

		if (success) {
			exceptionsPool.clear()
			converter.variablesPool.clear()
			converter.stackFramePool.removeAllOwnedBy(args.threadId.toLong())
		}

		ContinueResponse().apply {
			allThreadsContinued = allThreads
		}
	}
	
	override fun next(args: NextArguments) = async.execute {
		debuggee!!.threadByID(args.threadId.toLong())?.stepOver()
	}
	
	override fun stepIn(args: StepInArguments) = async.execute {
		debuggee!!.threadByID(args.threadId.toLong())?.stepInto()
	}
	
	override fun stepOut(args: StepOutArguments) = async.execute {
		debuggee!!.threadByID(args.threadId.toLong())?.stepOut()
	}
	
	override fun stepBack(args: StepBackArguments): CompletableFuture<Void> = notImplementedDAPMethod()
	
	override fun reverseContinue(args: ReverseContinueArguments): CompletableFuture<Void> = notImplementedDAPMethod()
	
	override fun restartFrame(args: RestartFrameArguments): CompletableFuture<Void> = notImplementedDAPMethod()
	
	override fun goto_(args: GotoArguments): CompletableFuture<Void> = notImplementedDAPMethod()
	
	override fun pause(args: PauseArguments) = async.execute {
		val threadId = args.threadId
		val success = debuggee!!.threadByID(threadId.toLong())?.pause()
		if (success ?: false) {
			// If successful
			sendStopEvent(threadId.toLong(),
				StoppedEventArgumentsReason.PAUSE
			)
		}
	}
	
	/*
	 * Stack traces, scopes and variables are computed synchronously
	 * to avoid race conditions when fetching elements from the pools
	 */
	
	override fun stackTrace(args: StackTraceArguments): CompletableFuture<StackTraceResponse> {
		val threadId = args.threadId
		return completedFuture(StackTraceResponse().apply {
			stackFrames = debuggee!!
				.threadByID(threadId.toLong())
				?.stackTrace()
				?.frames
				?.map { converter.toDAPStackFrame(it, threadId.toLong()) }
				?.toTypedArray()
				.orEmpty()
		})
	}
	
	override fun scopes(args: ScopesArguments) = completedFuture(
		ScopesResponse().apply {
			scopes = (converter.toInternalStackFrame(args.frameId.toLong())
					?: throw KotlinDAException("Could not find stackTrace with ID ${args.frameId}"))
				.scopes
				.map(converter::toDAPScope)
				.toTypedArray()
		}
	)
	
	override fun variables(args: VariablesArguments) = completedFuture(
		VariablesResponse().apply {
			variables = (args.variablesReference
				.toLong()
				.let(converter::toVariableTree)
					?: throw KotlinDAException("Could not find variablesReference with ID ${args.variablesReference}"))
				.childs
				?.map(converter::toDAPVariable)
				?.toTypedArray()
				.orEmpty()
		}
	)
	
	override fun setVariable(args: SetVariableArguments): CompletableFuture<SetVariableResponse> = notImplementedDAPMethod()
	
	override fun source(args: SourceArguments): CompletableFuture<SourceResponse> = notImplementedDAPMethod()
	
	override fun threads() = async.compute { onceDebuggeeIsPresent { debuggee ->
		debuggee.updateThreads()
		ThreadsResponse().apply {
			threads = debuggee.threads
				.asSequence()
				.map(converter::toDAPThread)
				.toList()
				.toTypedArray()
		}
	} }
	
	override fun modules(args: ModulesArguments): CompletableFuture<ModulesResponse> = notImplementedDAPMethod()
	
	override fun loadedSources(args: LoadedSourcesArguments): CompletableFuture<LoadedSourcesResponse> = notImplementedDAPMethod()
	
	override fun evaluate(args: EvaluateArguments): CompletableFuture<EvaluateResponse> = async.compute {
		val variable = (args.frameId
			.toLong()
			.let(converter::toInternalStackFrame)
				?: throw KotlinDAException("Could not find stack frame with ID ${args.frameId}"))
			.evaluate(args.expression)
			?.let(converter::toDAPVariable)

		EvaluateResponse().apply {
			result = variable?.value ?: "unknown"
			variablesReference = variable?.variablesReference ?: 0
		}
	}
	
	override fun stepInTargets(args: StepInTargetsArguments): CompletableFuture<StepInTargetsResponse> = notImplementedDAPMethod()
	
	override fun gotoTargets(args: GotoTargetsArguments): CompletableFuture<GotoTargetsResponse> = notImplementedDAPMethod()
	
	override fun completions(args: CompletionsArguments): CompletableFuture<CompletionsResponse> = async.compute {
		CompletionsResponse().apply {
			targets = (args.frameId
				.toLong()
				.let(converter::toInternalStackFrame)
					?: throw KotlinDAException("Could not find stack frame with ID ${args.frameId}"))
				.completions(args.text)
				.map(converter::toDAPCompletionItem)
				.toTypedArray()
		}
	}
	
	override fun exceptionInfo(args: ExceptionInfoArguments): CompletableFuture<ExceptionInfoResponse> = async.compute {
		val id = exceptionsPool.getIDsOwnedBy(args.threadId.toLong()).firstOrNull()
		val exception = id?.let { exceptionsPool.getByID(it) }
		ExceptionInfoResponse().apply {
			exceptionId = id?.toString() ?: ""
			description = exception?.description ?: "Unknown exception"
			breakMode = ExceptionBreakMode.ALWAYS
			details = exception?.let(converter::toDAPExceptionDetails)
		}
	}
	
	private fun connectLoggingBackend(client: IDebugProtocolClient) {
		val backend: (LogMessage) -> Unit = {
			client.output(OutputEventArguments().apply {
				category = OutputEventArgumentsCategory.CONSOLE
				output = "[${it.level}] ${it.message}\n"
			})
		}
		LOG.connectOutputBackend(backend)
		LOG.connectErrorBackend(backend)
	}
	
	private inline fun <T> onceDebuggeeIsPresent(body: (Debuggee) -> T): T {
		waitFor("debuggee") { debuggee != null }
		return body(debuggee!!)
	}
	
	private fun <T> notImplementedDAPMethod(): CompletableFuture<T> {
		TODO("not implemented yet")
	}
}
