package com.fwcd.ktda

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ThreadLocalRandom
import org.eclipse.lsp4j.debug.*
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import com.fwcd.ktda.util.KotlinDAException
import com.fwcd.ktda.util.AsyncExecutor
import com.fwcd.ktda.util.waitUntil
import com.fwcd.ktda.util.ObjectPool
import com.fwcd.ktda.classpath.findClassPath
import com.fwcd.ktda.jdi.JVMDebugSession
import com.fwcd.ktda.jdi.VMEventBus

typealias JDIBreakpointEvent = com.sun.jdi.event.BreakpointEvent
typealias JDIVMDeathEvent = com.sun.jdi.event.VMDeathEvent
typealias JDIStackFrame = com.sun.jdi.StackFrame

class KotlinDebugAdapter: IDebugProtocolServer {
	private val async = AsyncExecutor()
	private val launcherAsync = AsyncExecutor()
	
	private var project: Project? = null
	private var debugSession: JVMDebugSession? = null
	private var client: IDebugProtocolClient? = null
	
	private val stackFramePool = ObjectPool<Long, JDIStackFrame>() // Contains stack frames owned by thread ids
	private val breakpointManager = BreakpointManager()
	
	// TODO: This is a workaround for https://github.com/eclipse/lsp4j/issues/229
	// For more information, see launch() method
	private var configurationDoneResponse: CompletableFuture<Void>? = null
	
	override fun initialize(args: InitializeRequestArguments): CompletableFuture<Capabilities> = async.compute {
		val capabilities = Capabilities()
		// TODO: Configure capabilities
		// TODO: Configure debugger (for example whether lines are zero-indexed)
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
		
		project = Project(
			findClassPath(listOf(projectRoot)),
			mainClass,
			projectRoot
		)
		debugSession = JVMDebugSession(
			project!!,
			breakpointManager
		).apply { setupVMListeners(vmEvents) }
	}
	
	private fun setupVMListeners(events: VMEventBus) {
		events.stopListeners.add {
			client!!.exited(ExitedEventArguments().apply {
				// TODO: Use actual exitCode instead
				exitCode = 0L
			})
			LOG.info("Sent exit event")
		}
		events.subscribe(JDIBreakpointEvent::class) {
			// val breakpoint = breakpointManager.breakpointAt(it.jdiEvent.location())
			client!!.stopped(StoppedEventArguments().apply {
				reason = StoppedEventArgumentsReason.BREAKPOINT
				threadId = it.jdiEvent.thread().uniqueID()
			})
			it.resumeThreads = false
		}
	}
	
	override fun attach(args: Map<String, Any>): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun restart(args: RestartArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun disconnect(args: DisconnectArguments) = async.run {
		debugSession?.stop()
	}
	
	override fun setBreakpoints(args: SetBreakpointsArguments) = async.compute {
		LOG.info("${args.breakpoints.size} breakpoints found")
		
		// TODO: Support logpoints and conditional breakpoints
		// TODO: 0- or 1-indexed line numbers?
		
		val breakpoints = breakpointManager.setAllInSource(args.source, args.breakpoints)
		
		SetBreakpointsResponse().also {
			it.breakpoints = breakpoints.toTypedArray()
		}
	}
	
	override fun setFunctionBreakpoints(args: SetFunctionBreakpointsArguments): CompletableFuture<SetFunctionBreakpointsResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun setExceptionBreakpoints(args: SetExceptionBreakpointsArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun continue_(args: ContinueArguments): CompletableFuture<ContinueResponse> {
		return notImplementedDAPMethod()
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
	
	override fun pause(args: PauseArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun stackTrace(args: StackTraceArguments) = async.compute {
		val threadId = args.threadId
		
		StackTraceResponse().apply {
			stackFrames = debugSession!!.stackTrace(threadId)
				?.map { jdiFrame ->
					val location = jdiFrame.location()
					LOG.info("Source name ${location?.sourceName()}, source path ${location?.sourcePath()}")
					StackFrame().apply {
						id = stackFramePool.store(threadId, jdiFrame)
						name = location?.method()?.name() ?: "???"
						line = location?.lineNumber()?.toLong() ?: 0L
						column = 0L
						source = Source().apply {
							name = location?.sourceName() ?: "???"
							// TODO: Use source references to load locations in compiled classes
							path = location?.sourcePath()
								?.let(project!!.sourcesRoot::resolve)
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
		val jdiFrame = stackFramePool.getByID(id) ?: throw KotlinDAException("Could not find stack frame for ID " + id)
		
		VariablesResponse().apply {
			variables = jdiFrame.visibleVariables()
				.map { jdiVariable -> Variable().apply {
					name = jdiVariable.name()
					// TODO: Find a better string representation of variables
					value = jdiFrame.getValue(jdiVariable).toString()
					type = jdiVariable.type().signature()
					// TODO: Child variables
				} }
				.toTypedArray()
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
