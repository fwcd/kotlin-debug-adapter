package com.fwcd.ktda

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
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

class KotlinDebugAdapter: IDebugProtocolServer {
	private var client: IDebugProtocolClient? = null
	
	override fun initialize(args: InitializeRequestArguments): CompletableFuture<Capabilities> {
		val capabilities = Capabilities()
		// TODO: Configure capabilities
		// TODO: Configure debugger (for example whether lines are zero-indexed)
		return completedFuture(capabilities)
	}
	
	fun connect(client: IDebugProtocolClient) {
		this.client = client
		LOG.connectOutputBackend { output(it, OutputCategory.CONSOLE) }
		LOG.connectErrorBackend { output(it, OutputCategory.STDERR) }
		LOG.info("Connected to client")
	}
	
	override fun runInTerminal(args: RunInTerminalRequestArguments): CompletableFuture<RunInTerminalResponse> {
		return unimplemented()
	}
	
	override fun configurationDone(args: ConfigurationDoneArguments): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun launch(args: Map<String, Any>): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun attach(args: Map<String, Any>): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun restart(args: RestartArguments): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun disconnect(args: DisconnectArguments): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun setBreakpoints(args: SetBreakpointsArguments): CompletableFuture<SetBreakpointsResponse> {
		return unimplemented()
	}
	
	override fun setFunctionBreakpoints(args: SetFunctionBreakpointsArguments): CompletableFuture<SetFunctionBreakpointsResponse> {
		return unimplemented()
	}
	
	override fun setExceptionBreakpoints(args: SetExceptionBreakpointsArguments): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun continue_(args: ContinueArguments): CompletableFuture<ContinueResponse> {
		return unimplemented()
	}
	
	override fun next(args: NextArguments): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun stepIn(args: StepInArguments): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun stepOut(args: StepOutArguments): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun stepBack(args: StepBackArguments): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun reverseContinue(args: ReverseContinueArguments): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun restartFrame(args: RestartFrameArguments): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun goto_(args: GotoArguments): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun pause(args: PauseArguments): CompletableFuture<Void> {
		return unimplemented()
	}
	
	override fun stackTrace(args: StackTraceArguments): CompletableFuture<StackTraceResponse> {
		return unimplemented()
	}
	
	override fun scopes(args: ScopesArguments): CompletableFuture<ScopesResponse> {
		return unimplemented()
	}
	
	override fun variables(args: VariablesArguments): CompletableFuture<VariablesResponse> {
		return unimplemented()
	}
	
	override fun setVariable(args: SetVariableArguments): CompletableFuture<SetVariableResponse> {
		return unimplemented()
	}
	
	override fun source(args: SourceArguments): CompletableFuture<SourceResponse> {
		return unimplemented()
	}
	
	override fun threads(): CompletableFuture<ThreadsResponse> {
		return unimplemented()
	}
	
	override fun modules(args: ModulesArguments): CompletableFuture<ModulesResponse> {
		return unimplemented()
	}
	
	override fun loadedSources(args: LoadedSourcesArguments): CompletableFuture<LoadedSourcesResponse> {
		return unimplemented()
	}
	
	override fun evaluate(args: EvaluateArguments): CompletableFuture<EvaluateResponse> {
		return unimplemented()
	}
	
	override fun stepInTargets(args: StepInTargetsArguments): CompletableFuture<StepInTargetsResponse> {
		return unimplemented()
	}
	
	override fun gotoTargets(args: GotoTargetsArguments): CompletableFuture<GotoTargetsResponse> {
		return unimplemented()
	}
	
	override fun completions(args: CompletionsArguments): CompletableFuture<CompletionsResponse> {
		return unimplemented()
	}
	
	override fun exceptionInfo(args: ExceptionInfoArguments): CompletableFuture<ExceptionInfoResponse> {
		return unimplemented()
	}
	
	private fun output(msg: String, category: OutputCategory) {
		client?.let {
			val outputEvent = OutputEventArguments()
			outputEvent.output = msg
			outputEvent.category = category.value
			it.output(outputEvent)
		}
	}
	
	private fun <T> unimplemented(): CompletableFuture<T> {
		TODO("not implemented yet")
	}
}
