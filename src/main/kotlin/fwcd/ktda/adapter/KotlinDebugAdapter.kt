package fwcd.ktda.adapter

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ThreadLocalRandom
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import fwcd.ktda.LOG
import fwcd.ktda.LogMessage
import fwcd.ktda.util.KotlinDAException
import fwcd.ktda.util.AsyncExecutor
import fwcd.ktda.util.waitUntil
import fwcd.ktda.core.Debuggee
import fwcd.ktda.core.DebugContext
import fwcd.ktda.core.event.DebuggeeEventBus
import fwcd.ktda.core.event.BreakpointPauseEvent
import fwcd.ktda.core.event.StepPauseEvent
import fwcd.ktda.core.stack.StackFrame
import fwcd.ktda.core.launch.DebugLauncher
import fwcd.ktda.core.launch.LaunchConfiguration
import fwcd.ktda.core.launch.AttachConfiguration
import fwcd.ktda.core.breakpoint.ExceptionBreakpoint
import fwcd.ktda.classpath.findClassPath
import fwcd.ktda.classpath.findValidKtFilePath
import fwcd.ktda.jdi.event.VMEventBus

/** The debug server interface conforming to the Debug Adapter Protocol */
class KotlinDebugAdapter(
	private val launcher: DebugLauncher
): IDebugProtocolServer {
	private val async = AsyncExecutor()
	private val launcherAsync = AsyncExecutor()
	private val stdoutAsync = AsyncExecutor()
	private val stderrAsync = AsyncExecutor()
	
	private var debuggee: Debuggee? = null
	private var client: IDebugProtocolClient? = null
	private var converter = DAPConverter()
	private val context = DebugContext()
	
	// TODO: This is a workaround for https://github.com/eclipse/lsp4j/issues/229
	// For more information, see launch() method
	private var configurationDoneResponse: CompletableFuture<Void>? = null
	
	override fun initialize(args: InitializeRequestArguments): CompletableFuture<Capabilities> = async.compute {
		converter.lineConverter = LineNumberConverter(
			externalLineOffset = if (args.linesStartAt1) 0L else -1L
		)
		
		val capabilities = Capabilities()
		capabilities.supportsConfigurationDoneRequest = true
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
	
	override fun launch(args: Map<String, Any>) = launcherAsync.run {
		performInitialization()
		
		val projectRoot = (args["projectRoot"] as? String)?.let { Paths.get(it) }
			?: throw missingRequestArgument("launch", "projectRoot")
		
		val mainClass = (args["mainClass"] as? String)
			?: throw missingRequestArgument("launch", "mainClass")
		
		val config = LaunchConfiguration(
			findClassPath(listOf(projectRoot)),
			mainClass,
			projectRoot
		)
		debuggee = launcher.launch(
			config,
			context
		).also(::setupDebuggeeListeners)
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
		waitUntil { (configurationDoneResponse?.numberOfDependents ?: 0) != 0 }
		LOG.trace("Done waiting for configurationDoneResponse")
	}
	
	private fun setupDebuggeeListeners(debuggee: Debuggee) {
		val eventBus = debuggee.eventBus
		eventBus.stopListeners.add {
			// TODO: Use actual exitCode instead
			sendExitEvent(0L)
		}
		eventBus.breakpointListeners.add {
			sendStopEvent(it.threadID, StoppedEventArgumentsReason.BREAKPOINT)
		}
		eventBus.stepListeners.add {
			sendStopEvent(it.threadID, StoppedEventArgumentsReason.STEP)
		}
		eventBus.exceptionListeners.add {
			sendStopEvent(it.threadID, StoppedEventArgumentsReason.EXCEPTION)
		}
		stdoutAsync.run {
			debuggee.stdout?.let { pipeStreamToOutput(it, OutputEventArgumentsCategory.STDOUT) }
		}
		stderrAsync.run {
			debuggee.stderr?.let { pipeStreamToOutput(it, OutputEventArgumentsCategory.STDERR) }
		}
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
	
	private fun sendStopEvent(threadId: Long, reason: String) {
		client!!.stopped(StoppedEventArguments().also {
			it.reason = reason
			it.threadId = threadId
		})
	}
	
	private fun sendExitEvent(exitCode: Long) {
		client!!.exited(ExitedEventArguments().also {
			it.exitCode = exitCode
		})
		LOG.info("Sent exit event")
	}
	
	override fun attach(args: Map<String, Any>) = async.run {
		performInitialization()
		
		val projectRoot = (args["projectRoot"] as? String)?.let { Paths.get(it) }
			?: throw missingRequestArgument("launch", "projectRoot")
		
		val hostName = (args["hostName"] as? String)
			?: throw missingRequestArgument("attach", "hostName")
		
		val port = (args["port"] as? Int)
			?: throw missingRequestArgument("attach", "port")
		
		val timeout = (args["timeout"] as? Int)
			?: throw missingRequestArgument("attach", "timeout")
		
		debuggee = launcher.attach(
			AttachConfiguration(projectRoot, hostName, port, timeout),
			context
		).also(::setupDebuggeeListeners)
	}
	
	override fun restart(args: RestartArguments): CompletableFuture<Void> = notImplementedDAPMethod()
	
	override fun disconnect(args: DisconnectArguments) = async.run {
		debuggee?.stop()
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
	
	override fun setExceptionBreakpoints(args: SetExceptionBreakpointsArguments) = async.run {
		args.filters
			.map(converter::toInternalExceptionBreakpoint)
			.toSet()
			.let(context.breakpointManager.exceptionBreakpoints::setAll)
	}
	
	override fun continue_(args: ContinueArguments) = async.compute {
		val success = debuggee!!.threadByID(args.threadId)?.resume()
		if (success ?: false) {
			converter.variablesPool.clear()
			converter.stackFramePool.removeAllOwnedBy(args.threadId)
		}
		ContinueResponse().apply {
			allThreadsContinued = false
		}
	}
	
	override fun next(args: NextArguments) = async.run {
		debuggee!!.threadByID(args.threadId)?.stepOver()
	}
	
	override fun stepIn(args: StepInArguments) = async.run {
		debuggee!!.threadByID(args.threadId)?.stepInto()
	}
	
	override fun stepOut(args: StepOutArguments) = async.run {
		debuggee!!.threadByID(args.threadId)?.stepOut()
	}
	
	override fun stepBack(args: StepBackArguments): CompletableFuture<Void> = notImplementedDAPMethod()
	
	override fun reverseContinue(args: ReverseContinueArguments): CompletableFuture<Void> = notImplementedDAPMethod()
	
	override fun restartFrame(args: RestartFrameArguments): CompletableFuture<Void> = notImplementedDAPMethod()
	
	override fun goto_(args: GotoArguments): CompletableFuture<Void> = notImplementedDAPMethod()
	
	override fun pause(args: PauseArguments) = async.run {
		val threadId = args.threadId
		val success = debuggee!!.threadByID(threadId)?.pause()
		if (success ?: false) {
			// If successful
			sendStopEvent(threadId,
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
				.threadByID(threadId)
				?.stackTrace()
				?.frames
				?.map { converter.toDAPStackFrame(it, threadId) }
				?.toTypedArray()
				.orEmpty()
		})
	}
	
	override fun scopes(args: ScopesArguments) = completedFuture(
		ScopesResponse().apply {
			scopes = (converter.toInternalStackFrame(args.frameId)
					?: throw KotlinDAException("Could not find stackTrace with ID ${args.frameId}"))
				.scopes
				.map(converter::toDAPScope)
				.toTypedArray()
		}
	)
	
	override fun variables(args: VariablesArguments) = completedFuture(
		VariablesResponse().apply {
			variables = (args.variablesReference
				.let(converter::toVariableTree)
					?: throw KotlinDAException("Could not find variablesReference with ID ${args.variablesReference}"))
				.childs
				.map(converter::toDAPVariable)
				.toTypedArray()
		}
	)
	
	override fun setVariable(args: SetVariableArguments): CompletableFuture<SetVariableResponse> = notImplementedDAPMethod()
	
	override fun source(args: SourceArguments): CompletableFuture<SourceResponse> = notImplementedDAPMethod()
	
	override fun threads() = async.compute { onceDebuggeeIsPresent { debuggee ->
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
	
	override fun evaluate(args: EvaluateArguments): CompletableFuture<EvaluateResponse> = notImplementedDAPMethod()
	
	override fun stepInTargets(args: StepInTargetsArguments): CompletableFuture<StepInTargetsResponse> = notImplementedDAPMethod()
	
	override fun gotoTargets(args: GotoTargetsArguments): CompletableFuture<GotoTargetsResponse> = notImplementedDAPMethod()
	
	override fun completions(args: CompletionsArguments): CompletableFuture<CompletionsResponse> = notImplementedDAPMethod()
	
	override fun exceptionInfo(args: ExceptionInfoArguments): CompletableFuture<ExceptionInfoResponse> = notImplementedDAPMethod()
	
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
		waitUntil { debuggee != null }
		return body(debuggee!!)
	}
	
	private fun <T> notImplementedDAPMethod(): CompletableFuture<T> {
		TODO("not implemented yet")
	}
}
