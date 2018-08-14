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
import org.eclipse.lsp4j.debug.VariablesArguments
import org.eclipse.lsp4j.debug.VariablesResponse
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient

class KotlinDebugAdapter: IDebugProtocolServer {
	@JsonRequest
	override fun initialize(args: InitializeRequestArguments): CompletableFuture<Capabilities> {
		val capabilities = Capabilities()
		// TODO
		return completedFuture(capabilities)
	}
	
	fun connect(client: IDebugProtocolClient) {
		// TODO: Add client hooks here
	}
	
	@JsonRequest
	override fun runInTerminal(args: RunInTerminalRequestArguments): CompletableFuture<RunInTerminalResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun configurationDone(args: ConfigurationDoneArguments): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun launch(args: Map<String, Any>): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun attach(args: Map<String, Any>): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun restart(args: RestartArguments): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun disconnect(args: DisconnectArguments): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun setBreakpoints(args: SetBreakpointsArguments): CompletableFuture<SetBreakpointsResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun setFunctionBreakpoints(args: SetFunctionBreakpointsArguments): CompletableFuture<SetFunctionBreakpointsResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun setExceptionBreakpoints(args: SetExceptionBreakpointsArguments): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest(value = "continue")
	override fun continue_(args: ContinueArguments): CompletableFuture<ContinueResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun next(args: NextArguments): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun stepIn(args: StepInArguments): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun stepOut(args: StepOutArguments): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun stepBack(args: StepBackArguments): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun reverseContinue(args: ReverseContinueArguments): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun restartFrame(args: RestartFrameArguments): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest(value = "goto")
	override fun goto_(args: GotoArguments): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun pause(args: PauseArguments): CompletableFuture<Void> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun stackTrace(args: StackTraceArguments): CompletableFuture<StackTraceResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun scopes(args: ScopesArguments): CompletableFuture<ScopesResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun variables(args: VariablesArguments): CompletableFuture<VariablesResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun setVariable(args: SetVariableArguments): CompletableFuture<SetVariableResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun source(args: SourceArguments): CompletableFuture<SourceResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun threads(): CompletableFuture<ThreadsResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun modules(args: ModulesArguments): CompletableFuture<ModulesResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun loadedSources(args: LoadedSourcesArguments): CompletableFuture<LoadedSourcesResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun evaluate(args: EvaluateArguments): CompletableFuture<EvaluateResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun stepInTargets(args: StepInTargetsArguments): CompletableFuture<StepInTargetsResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun gotoTargets(args: GotoTargetsArguments): CompletableFuture<GotoTargetsResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun completions(args: CompletionsArguments): CompletableFuture<CompletionsResponse> {
		TODO("not implemented yet")
	}
	
	@JsonRequest
	override fun exceptionInfo(args: ExceptionInfoArguments): CompletableFuture<ExceptionInfoResponse> {
		TODO("not implemented yet")
	}
}
