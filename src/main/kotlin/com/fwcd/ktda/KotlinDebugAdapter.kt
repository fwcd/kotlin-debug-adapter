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
import com.fwcd.ktda.classpath.findClassPath

class KotlinDebugAdapter: IDebugProtocolServer {
	private val async = AsyncExecutor()
	private var debugSession: JVMDebugSession? = null
	private var client: IDebugProtocolClient? = null
	
	override fun initialize(args: InitializeRequestArguments): CompletableFuture<Capabilities> = async.compute {
		val capabilities = Capabilities()
		// TODO: Configure capabilities
		// TODO: Configure debugger (for example whether lines are zero-indexed)
		client?.initialized()
		capabilities
	}
	
	fun connect(client: IDebugProtocolClient) {
		this.client = client
		LOG.info("Connected to client")
	}
	
	override fun runInTerminal(args: RunInTerminalRequestArguments): CompletableFuture<RunInTerminalResponse> {
		return notImplementedDAPMethod()
	}
	
	override fun configurationDone(args: ConfigurationDoneArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun launch(args: Map<String, Any>): CompletableFuture<Void> = async.run {
		val projectRoot = args["projectRoot"] as? String
		if (projectRoot == null) throw KotlinDAException("Sent 'launch' request to debug adapter without the required 'projectRoot' argument")
		
		val mainClass = args["mainClass"] as? String
		if (mainClass == null) throw KotlinDAException("Sent 'launch' request to debug adapter without the required 'mainClass' argument")
		
		debugSession = JVMDebugSession(
			findClassPath(listOf(Paths.get(projectRoot))),
			mainClass
		).apply {
			start()
		}
	}
	
	override fun attach(args: Map<String, Any>): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun restart(args: RestartArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun disconnect(args: DisconnectArguments): CompletableFuture<Void> = async.run {
		debugSession?.stop()
	}
	
	override fun setBreakpoints(args: SetBreakpointsArguments): CompletableFuture<SetBreakpointsResponse> {
		return notImplementedDAPMethod()
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
	
	override fun next(args: NextArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun stepIn(args: StepInArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
	}
	
	override fun stepOut(args: StepOutArguments): CompletableFuture<Void> {
		return notImplementedDAPMethod()
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
	
	override fun threads(): CompletableFuture<ThreadsResponse> {
		return notImplementedDAPMethod()
	}
	
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
	
	private fun <T> notImplementedDAPMethod(): CompletableFuture<T> {
		TODO("not implemented yet")
	}
}
