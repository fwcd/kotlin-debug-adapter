package com.fwcd.ktda.adapter

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
import com.fwcd.ktda.LOG
import com.fwcd.ktda.LogMessage
import com.fwcd.ktda.util.KotlinDAException
import com.fwcd.ktda.util.AsyncExecutor
import com.fwcd.ktda.util.waitUntil
import com.fwcd.ktda.core.Debuggee
import com.fwcd.ktda.core.DebugContext
import com.fwcd.ktda.core.event.DebuggeeEventBus
import com.fwcd.ktda.core.event.BreakpointPauseEvent
import com.fwcd.ktda.core.event.StepPauseEvent
import com.fwcd.ktda.core.stack.StackFrame
import com.fwcd.ktda.core.launch.DebugLauncher
import com.fwcd.ktda.core.launch.LaunchConfiguration
import com.fwcd.ktda.core.breakpoint.ExceptionBreakpoint
import com.fwcd.ktda.classpath.findClassPath
import com.fwcd.ktda.classpath.findValidKtFilePath
import com.fwcd.ktda.jdi.event.VMEventBus

/** The debug server interface conforming to the Debug Adapter Protocol */
class KotlinDebugAdapter(
	private val launcher: DebugLauncher
): IDebugProtocolServer {
	private val async = AsyncExecutor()
	private val launcherAsync = AsyncExecutor()
	private val stdoutAsync = AsyncExecutor()
	private val stderrAsync = AsyncExecutor()
	
	private var config: LaunchConfiguration? = null
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
	
	override fun runInTerminal(args: RunInTerminalRequestArguments): CompletableFuture<RunInTerminalResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun configurationDone(args: ConfigurationDoneArguments?): CompletableFuture<Void> {
		LOG.trace("Got configurationDone request")
		val response = CompletableFuture<Void>()
		configurationDoneResponse = response
		return response
	}
	
	override fun launch(args: Map<String, Any>) = launcherAsync.run {
		client!!.initialized()
		
		// Wait for configurationDone response to fully return
		// as sketched in https://github.com/Microsoft/vscode/issues/4902#issuecomment-368583522
		// TODO: Find a cleaner solution once https://github.com/eclipse/lsp4j/issues/229 is resolved
		// (LSP4J does currently not provide a mechanism to hook into the request/response machinery)
		
		LOG.trace("Waiting for configurationDoneResponse")
		waitUntil { (configurationDoneResponse?.numberOfDependents ?: 0) != 0 }
		LOG.trace("Done waiting for configurationDoneResponse")
		
		val projectRoot = (args["projectRoot"] as? String)?.let { Paths.get(it) }
		if (projectRoot == null) throw KotlinDAException("Sent 'launch' request to debug adapter without the required 'projectRoot' argument")
		
		val mainClass = args["mainClass"] as? String
		if (mainClass == null) throw KotlinDAException("Sent 'launch' request to debug adapter without the required 'mainClass' argument")
		
		config = LaunchConfiguration(
			findClassPath(listOf(projectRoot)),
			mainClass,
			projectRoot
		)
		debuggee = launcher.launch(
			config!!,
			context
		).also(::setupDebuggeeListeners)
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
	
	override fun attach(args: Map<String, Any>): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun restart(args: RestartArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
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
	
	override fun setFunctionBreakpoints(args: SetFunctionBreakpointsArguments): CompletableFuture<SetFunctionBreakpointsResponse> {
		return notImplementedDAPMethod()
	}
	
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
	
	override fun stepBack(args: StepBackArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun reverseContinue(args: ReverseContinueArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun restartFrame(args: RestartFrameArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun goto_(args: GotoArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
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
	
	override fun setVariable(args: SetVariableArguments): CompletableFuture<SetVariableResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun source(args: SourceArguments): CompletableFuture<SourceResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun threads() = async.compute { onceDebuggeeIsPresent { debuggee ->
		ThreadsResponse().apply {
			threads = debuggee.threads
				.asSequence()
				.map(converter::toDAPThread)
				.toList()
				.toTypedArray()
		}
	} }
	
	override fun modules(args: ModulesArguments): CompletableFuture<ModulesResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun loadedSources(args: LoadedSourcesArguments): CompletableFuture<LoadedSourcesResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun evaluate(args: EvaluateArguments): CompletableFuture<EvaluateResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun stepInTargets(args: StepInTargetsArguments): CompletableFuture<StepInTargetsResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun gotoTargets(args: GotoTargetsArguments): CompletableFuture<GotoTargetsResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun completions(args: CompletionsArguments): CompletableFuture<CompletionsResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun exceptionInfo(args: ExceptionInfoArguments): CompletableFuture<ExceptionInfoResponse> {
		return notImplementedDAPMethod()
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
		waitUntil { debuggee != null }
		return body(debuggee!!)
	}
	
	private fun <T> notImplementedDAPMethod(): CompletableFuture<T> {
		TODO("not implemented yet")
	}
}
