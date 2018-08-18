package com.fwcd.ktda

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.nio.file.Paths
import org.eclipse.lsp4j.debug.Capabilities
import org.eclipse.lsp4j.debug.CompletionsArguments
import org.eclipse.lsp4j.debug.CompletionsResponse
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments
import org.eclipse.lsp4j.debug.ContinueArguments
import org.eclipse.lsp4j.debug.ContinueResponse
import org.eclipse.lsp4j.debug.DisconnectArguments
import org.eclipse.lsp4j.debug.EvaluateArguments
import org.eclipse.lsp4j.debug.EvaluateResponse
import org.eclipse.lsp4j.debug.ExceptionInfoArguments
import org.eclipse.lsp4j.debug.ExceptionInfoResponse
import org.eclipse.lsp4j.debug.ExitedEventArguments
import org.eclipse.lsp4j.debug.GotoArguments
import org.eclipse.lsp4j.debug.GotoTargetsArguments
import org.eclipse.lsp4j.debug.GotoTargetsResponse
import org.eclipse.lsp4j.debug.InitializeRequestArguments
import org.eclipse.lsp4j.debug.LoadedSourcesArguments
import org.eclipse.lsp4j.debug.LoadedSourcesResponse
import org.eclipse.lsp4j.debug.ModulesArguments
import org.eclipse.lsp4j.debug.ModulesResponse
import org.eclipse.lsp4j.debug.NextArguments
import org.eclipse.lsp4j.debug.PauseArguments
import org.eclipse.lsp4j.debug.RestartArguments
import org.eclipse.lsp4j.debug.RestartFrameArguments
import org.eclipse.lsp4j.debug.ReverseContinueArguments
import org.eclipse.lsp4j.debug.RunInTerminalRequestArguments
import org.eclipse.lsp4j.debug.RunInTerminalResponse
import org.eclipse.lsp4j.debug.ScopesArguments
import org.eclipse.lsp4j.debug.ScopesResponse
import org.eclipse.lsp4j.debug.SetBreakpointsArguments
import org.eclipse.lsp4j.debug.SetBreakpointsResponse
import org.eclipse.lsp4j.debug.SetExceptionBreakpointsArguments
import org.eclipse.lsp4j.debug.SetFunctionBreakpointsArguments
import org.eclipse.lsp4j.debug.SetFunctionBreakpointsResponse
import org.eclipse.lsp4j.debug.SetVariableArguments
import org.eclipse.lsp4j.debug.SetVariableResponse
import org.eclipse.lsp4j.debug.SourceArguments
import org.eclipse.lsp4j.debug.SourceResponse
import org.eclipse.lsp4j.debug.StackTraceArguments
import org.eclipse.lsp4j.debug.StackTraceResponse
import org.eclipse.lsp4j.debug.StepBackArguments
import org.eclipse.lsp4j.debug.StepInArguments
import org.eclipse.lsp4j.debug.StepInTargetsArguments
import org.eclipse.lsp4j.debug.StepInTargetsResponse
import org.eclipse.lsp4j.debug.StepOutArguments
import org.eclipse.lsp4j.debug.ThreadsResponse
import org.eclipse.lsp4j.debug.OutputEventArguments
import org.eclipse.lsp4j.debug.VariablesArguments
import org.eclipse.lsp4j.debug.VariablesResponse
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import com.fwcd.ktda.util.KotlinDAException
import com.fwcd.ktda.util.AsyncExecutor
import com.fwcd.ktda.util.waitUntil
import com.fwcd.ktda.classpath.findClassPath
import com.fwcd.ktda.jdi.JVMDebugSession
import com.fwcd.ktda.jdi.VMEventBus

class KotlinDebugAdapter: IDebugProtocolServer {
	private val async = AsyncExecutor()
	private val launcherAsync = AsyncExecutor()
	private var debugSession: JVMDebugSession? = null
	private var client: IDebugProtocolClient? = null
	
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
		
		debugSession = JVMDebugSession(
			findClassPath(listOf(projectRoot)),
			mainClass,
			projectRoot
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
	
	override fun setBreakpoints(args: SetBreakpointsArguments): CompletableFuture<SetBreakpointsResponse> = async.compute {
		LOG.info("${args.breakpoints.size} breakpoints found")
		// TODO: Register these breakpoints
		null
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
	
	override fun stackTrace(args: StackTraceArguments): CompletableFuture<StackTraceResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun scopes(args: ScopesArguments): CompletableFuture<ScopesResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun variables(args: VariablesArguments): CompletableFuture<VariablesResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun setVariable(args: SetVariableArguments): CompletableFuture<SetVariableResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun source(args: SourceArguments): CompletableFuture<SourceResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun threads(): CompletableFuture<ThreadsResponse> = async.compute { withDebugSession {
		it.allThreads()
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
	
	private inline fun <T> withDebugSession(body: (JVMDebugSession) -> T): T {
		waitUntil { debugSession != null }
		return body(debugSession!!)
	}
	
	private fun <T> notImplementedDAPMethod(): CompletableFuture<T> {
		TODO("not implemented yet")
	}
}
