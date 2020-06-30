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
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.nullValue
import org.hamcrest.Matchers.not
import java.util.concurrent.CountDownLatch

/**
 * Tests a very basic debugging scenario
 * using a sample application.
 */
class SampleWorkspaceTest : DebugAdapterTestFixture("sample-workspace", "sample.workspace.AppKt", "-Dtest=testVmArgs") {
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
                line = 8
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

            assertThat(members.map { Pair(it.name, it.value) }, containsInAnyOrder(Pair("member", "\"testVmArgs\"")))
        } catch (e: Throwable) {
            asyncException = e
        } finally {
            latch.countDown()
        }
    }
}
