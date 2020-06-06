package org.javacs.ktda

import org.eclipse.lsp4j.debug.ScopesArguments
import org.eclipse.lsp4j.debug.SetBreakpointsArguments
import org.eclipse.lsp4j.debug.Source
import org.eclipse.lsp4j.debug.SourceBreakpoint
import org.eclipse.lsp4j.debug.StackFrame
import org.eclipse.lsp4j.debug.StackTraceArguments
import org.eclipse.lsp4j.debug.StoppedEventArguments
import org.eclipse.lsp4j.debug.VariablesArguments
import org.junit.Assert.assertThat
import org.junit.Test
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.hamcrest.Matchers.not
import java.util.concurrent.Semaphore

/**
 * Tests a very basic debugging scenario
 * using a sample application.
 */
class SampleWorkspaceTest : DebugAdapterTestFixture("sample-workspace", "sample.workspace.AppKt") {
    private val semaphore = Semaphore(0)

    @Test fun testBreakpointsAndVariables() {
        debugAdapter.setBreakpoints(SetBreakpointsArguments().apply {
            source = Source().apply {
                name = "App.kt"
                path = absoluteWorkspaceRoot
                    .resolve("src")
                    .resolve("main")
                    .resolve("kotlin")
                    .resolve("sample")
                    .resolve("workspace")
                    .resolve("App.kt")
                    .toString()
            }
            breakpoints = arrayOf(SourceBreakpoint().apply {
                line = 8
            })
        }).join()
        launch()
        semaphore.acquire() // Wait for the end
    }

    override fun stopped(args: StoppedEventArguments) {
        try {
            assertThat(args.reason, equalTo("breakpoint"))

            // Query information about the debuggee's current state
            val stackTrace = debugAdapter.stackTrace(StackTraceArguments().apply {
                threadId = args.threadId
            }).join()
            val topFrame = stackTrace.stackFrames.first()
            val scopes = debugAdapter.scopes(ScopesArguments().apply {
                frameId = topFrame.id
            }).join()
            val scope = scopes.scopes.first()
            val variables = debugAdapter.variables(VariablesArguments().apply {
                variablesReference = scope.variablesReference
            }).join()
            val receiver = variables.variables.find { it.name == "this" }
            
            assertThat(variables.variables.map { Pair(it.name, it.value) }, contains(Pair("local", "123")))
            assertThat(receiver, not(nullValue()))

            val members = debugAdapter.variables(VariablesArguments().apply {
                variablesReference = receiver!!.variablesReference
            }).join()

            assertThat(members.variables.map { Pair(it.name, it.value) }, contains(Pair("member", "test")))
        } finally {
            semaphore.release()
        }
    }
}
