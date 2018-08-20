package com.fwcd.ktda.adapter

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ThreadLocalRandom
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import com.fwcd.ktda.LOG
import com.fwcd.ktda.util.KotlinDAException
import com.fwcd.ktda.util.AsyncExecutor
import com.fwcd.ktda.util.waitUntil
import com.fwcd.ktda.util.ObjectPool
import com.fwcd.ktda.core.Debuggee
import com.fwcd.ktda.core.DebugContext
import com.fwcd.ktda.core.event.DebuggeeEventBus
import com.fwcd.ktda.core.event.BreakpointPauseEvent
import com.fwcd.ktda.core.event.StepPauseEvent
import com.fwcd.ktda.core.stack.StackFrame
import com.fwcd.ktda.core.launch.DebugLauncher
import com.fwcd.ktda.core.launch.LaunchConfiguration
import com.fwcd.ktda.classpath.findClassPath
import com.fwcd.ktda.classpath.findValidKtFilePath
import com.fwcd.ktda.jdi.event.VMEventBus

/** The debug server interface conforming to the Debug Adapter Protocol */
class KotlinDebugAdapter(
	private val launcher: DebugLauncher
): IDebugProtocolServer {
	private val async = AsyncExecutor()
	private val launcherAsync = AsyncExecutor()
	
	private var config: LaunchConfiguration? = null
	private var debuggee: Debuggee? = null
	private var client: IDebugProtocolClient? = null
	private var converter = DAPConversionFacade()
	
	private val context = DebugContext()
	private val stackFramePool = ObjectPool<Long, StackFrame>() // Contains stack frames owned by thread ids
	
	// TODO: This is a workaround for https://github.com/eclipse/lsp4j/issues/229
	// For more information, see launch() method
	private var configurationDoneResponse: CompletableFuture<Void>? = null
	
	override fun initialize(args: InitializeRequestArguments): CompletableFuture<Capabilities> = async.compute {
		converter.lineConverter = LineNumberConverter(
			externalLineOffset = if (args.linesStartAt1) 0 else -1
		)
		
		val capabilities = Capabilities()
		capabilities.supportsConfigurationDoneRequest = true
		
		LOG.info("Returning capabilities...")
		capabilities
	}
	
	fun connect(client: IDebugProtocolClient) {
		this.client = client
		client.initialized()
		LOG.info("Connected to client")
	}
	
	override fun runInTerminal(args: RunInTerminalRequestArguments): CompletableFuture<RunInTerminalResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun configurationDone(args: ConfigurationDoneArguments?): CompletableFuture<Void> {
		LOG.info("Got configurationDone request")
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
		
		LOG.info("Waiting for configurationDoneResponse")
		waitUntil { (configurationDoneResponse?.numberOfDependents ?: 0) != 0 }
		LOG.info("Done waiting for configurationDoneResponse")
		
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
		).apply { setupDebuggeeListeners(eventBus) }
	}
	
	private fun setupDebuggeeListeners(eventBus: DebuggeeEventBus) {
		eventBus.stopListeners.add {
			// TODO: Use actual exitCode instead
			sendExitEvent(0L)
		}
		eventBus.breakpointListeners.add {
			sendStopEvent(
				it.threadID,
				StoppedEventArgumentsReason.BREAKPOINT
			)
		}
		eventBus.stepListeners.add {
			sendStopEvent(
				it.threadID,
				StoppedEventArgumentsReason.STEP
			)
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
		LOG.info("${args.breakpoints.size} breakpoints found")
		
		// TODO: Support logpoints and conditional breakpoints
		// TODO: 0- or 1-indexed line numbers?
		
		val placedBreakpoints = context.breakpointManager.setAllInSource(args.source, args.breakpoints)
		
		SetBreakpointsResponse().apply {
			breakpoints = placedBreakpoints.toTypedArray()
		}
	}
	
	override fun setFunctionBreakpoints(args: SetFunctionBreakpointsArguments): CompletableFuture<SetFunctionBreakpointsResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun setExceptionBreakpoints(args: SetExceptionBreakpointsArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun continue_(args: ContinueArguments) = async.compute {
		debugSession!!.resumeThread(args.threadId)
		ContinueResponse().apply {
			allThreadsContinued = false
		}
	}
	
	override fun next(args: NextArguments) = async.run {
		debugSession?.stepOver(args.threadId)
	}
	
	override fun stepIn(args: StepInArguments) = async.run {
		debugSession?.stepInto(args.threadId)
	}
	
	override fun stepOut(args: StepOutArguments) = async.run {
		debugSession?.stepOut(args.threadId)
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
		debugSession!!.pauseThread(threadId)?.let {
			// If successful
			sendStopEvent(
				threadId,
				StoppedEventArgumentsReason.PAUSE
			)
		}
	}
	
	override fun stackTrace(args: StackTraceArguments) = async.compute {
		val threadId = args.threadId
		stackFramePool.removeAllOwnedBy(threadId)
		
		StackTraceResponse().apply {
			stackFrames = debugSession!!.stackTrace(threadId)
				?.map { jdiFrame ->
					val location = jdiFrame.location()
					StackFrame().apply {
						id = stackFramePool.store(threadId, jdiFrame)
						name = location?.method()?.name() ?: "???"
						line = location?.lineNumber()?.toLong()?.let { it + lineOffset } ?: 0L
						column = 0L
						source = Source().apply {
							name = location?.sourceName() ?: "???"
							// TODO: Use source references to load locations in compiled classes
							path = location?.sourcePath()
								?.let(config!!.sourcesRoot::resolve)
								?.let(::findValidKtFilePath)
								?.toAbsolutePath()
								?.toString()
								?: "???"
						}
					}
				}?.toTypedArray()
				.orEmpty()
		}
	}
	
	override fun scopes(args: ScopesArguments) = async.compute {
		ScopesResponse().apply {
			scopes = arrayOf(Scope().apply {
				name = "Locals"
				variablesReference = args.frameId
				expensive = false // TODO
			})
		}
	}
	
	override fun variables(args: VariablesArguments) = async.compute {
		val id = args.variablesReference
		val jdiFrame = stackFramePool.getByID(id)
		
		VariablesResponse().apply {
			variables = try {
				jdiFrame
					?.visibleVariables()
					?.map { jdiVariable -> Variable().apply {
						name = jdiVariable.name()
						// TODO: Find a better string representation of variables
						value = jdiFrame.getValue(jdiVariable).toString()
						type = jdiVariable.type().signature()
						// TODO: Child variables
					} }
					?.toTypedArray()
			} catch (e: JDIAbsentInformationException) {
				null
			}.orEmpty()
		}
	}
	
	override fun setVariable(args: SetVariableArguments): CompletableFuture<SetVariableResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun source(args: SourceArguments): CompletableFuture<SourceResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun threads() = async.compute { onceDebugSessionIsPresent { session ->
		session.allThreads()
			.map { org.eclipse.lsp4j.debug.Thread().apply {
				name = it.name()
				id = it.uniqueID()
			} }
			.let { ThreadsResponse().apply {
				threads = it.toTypedArray()
			} }
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
	
	private inline fun <T> onceDebugSessionIsPresent(body: (JVMDebugSession) -> T): T {
		waitUntil { debugSession != null }
		return body(debugSession!!)
	}
	
	private fun <T> notImplementedDAPMethod(): CompletableFuture<T> {
		TODO("not implemented yet")
	}
}
