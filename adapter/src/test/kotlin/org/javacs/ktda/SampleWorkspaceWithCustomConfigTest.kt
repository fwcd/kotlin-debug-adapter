package org.javacs.ktda

import org.eclipse.lsp4j.debug.ScopesArguments
import org.eclipse.lsp4j.debug.SetBreakpointsArguments
import org.eclipse.lsp4j.debug.Source
import org.eclipse.lsp4j.debug.SourceBreakpoint
import org.eclipse.lsp4j.debug.StackFrame
import org.eclipse.lsp4j.debug.StackTraceArguments
import org.eclipse.lsp4j.debug.StoppedEventArguments
import org.eclipse.lsp4j.debug.VariablesArguments
import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Test
import org.javacs.kt.LOG
import java.util.concurrent.CountDownLatch

/**
 * Tests a very basic debugging scenario
 * using a sample application.
 */
class SampleWorkspaceWithCustomConfigTest : DebugAdapterTestFixture(
        "sample-workspace", "sample.workspace.AppKt",
        vmArguments = "-Dfoo=bar", cwd = "/tmp", envs = mapOf("MSG" to "hello")) {
    private val latch = CountDownLatch(1)
    private var asyncException: Throwable? = null

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
                line = 11
            })
        }).join()

        launch()
        latch.await() // Wait for the breakpoint event to finish
        asyncException?.let { throw it }
    }

    override fun stopped(args: StoppedEventArguments) {
        try {
            assertThat(args.reason, equalTo("breakpoint"))

            // Query information about the debuggee's current state
            val stackTrace = debugAdapter.stackTrace(StackTraceArguments().apply {
                threadId = args.threadId
            }).join()
            val locals = stackTrace.stackFrames.asSequence().flatMap {
                debugAdapter.scopes(ScopesArguments().apply {
                    frameId = it.id
                }).join().scopes.asSequence().flatMap {
                    debugAdapter.variables(VariablesArguments().apply {
                        variablesReference = it.variablesReference
                    }).join().variables.asSequence()
                }
            }.toList()
            val receiver = locals.find { it.name == "this" }
            
            assertThat(locals.map { Pair(it.name, it.value) }, hasItem(Pair("local", "123")))
            assertThat(receiver, not(nullValue()))

            val members = debugAdapter.variables(VariablesArguments().apply {
                variablesReference = receiver!!.variablesReference
            }).join().variables

            val memberMap = members.fold(mutableMapOf<String, String?>()) {
                map, v ->
                    map[v.name] = v.value
                    map
            }

            assertThat(memberMap["member"], equalTo(""""test""""))
            assertThat(memberMap["foo"], equalTo(""""bar""""))
            assertThat(memberMap["cwd"]?.trim('"'), containsString("/tmp"))
            assertThat(memberMap["msg"], equalTo(""""hello""""))

        } catch (e: Throwable) {
            asyncException = e
        } finally {
            latch.countDown()
        }
    }
}
