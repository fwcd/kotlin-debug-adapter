package org.javacs.ktda

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.eclipse.lsp4j.debug.*
import java.nio.file.Path
import java.nio.file.Paths
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.hamcrest.Matchers
import org.javacs.ktda.adapter.KotlinDebugAdapter
import org.javacs.ktda.jdi.launch.JDILauncher
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import java.lang.Thread
import java.util.concurrent.CountDownLatch

/**
 * Defines the basic debugging scenarios
 * that should be tested on all supported sample applications.
 */
abstract class GlobalWorkspaceTest(
    relativeWorkspaceRoot: String,
    private val mainClass: String,
    private val defaultVmArguments: String = "",
    private val buildCommand: Pair<String, String>
) {
    val absoluteWorkspaceRoot: Path =
        Paths.get(GlobalWorkspaceTest::class.java.getResource("/Anchor.txt").toURI()).parent.resolve(
            relativeWorkspaceRoot
        )
    var client = mockk<IDebugProtocolClient>(relaxed = true)
    lateinit var debugAdapter: KotlinDebugAdapter
    val latch = CountDownLatch(1)

    @Before
    fun startDebugAdapter() {
        // Reset the mock debug protocol client
        client = mockk(relaxed = true)

        // Build the project first
        val process = ProcessBuilder(buildCommand.first, buildCommand.second)
            .directory(absoluteWorkspaceRoot.toFile())
            .inheritIO()
            .start()
        process.waitFor()
        assertThat(process.exitValue(), equalTo(0))

        debugAdapter = JDILauncher()
            .let(::KotlinDebugAdapter)
            .also {
                it.connect(client)
                val configDone = it.configurationDone(ConfigurationDoneArguments())
                it.initialize(InitializeRequestArguments().apply {
                    adapterID = "test-debug-adapter"
                    linesStartAt1 = true
                    columnsStartAt1 = true
                }).join()
                // Slightly hacky workaround to ensure someone is
                // waiting on the ConfigurationDoneResponse. See
                // KotlinDebugAdapter.kt:performInitialization for
                // details.
                Thread {
                    configDone.join()
                }.start()
                // Wait until the thread has blocked on the future
                while (configDone.numberOfDependents == 0) {
                    Thread.sleep(100)
                }
            }

        // Print output on commandline for test debugging
        slot<OutputEventArguments>().also { outputEvent ->
            every { client.output(capture(outputEvent)) }.answers { println(outputEvent.captured.output) }
        }
        // And release the latch when the debug session is terminated/Done
        every { client.terminated(any()) }.answers { latch.countDown() }
    }

    @After
    fun closeDebugAdapter() {
        debugAdapter.disconnect(DisconnectArguments()).join()
    }

    @Test
    fun `Breakpoints should be hit and variables recorded`() {
        // Given: We set a breakpoint
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
        var locals: List<Variable> = emptyList()

        // TODO: We need this workaround because for some reason when the breakpoint is hit two StoppedEvents are send
        var times = 0
        // When we hit the breakpoint
        slot<StoppedEventArguments>().also { slot ->
            every { client.stopped(capture(slot)) } answers {
                if (times == 0) {
                    // We query information about the debuggee's current state
                    val stackTrace = debugAdapter.stackTrace(StackTraceArguments().apply {
                        threadId = slot.captured.threadId
                    }).join()
                    // And collect a list of current variable values
                    locals = stackTrace.stackFrames.asSequence().flatMap {
                        debugAdapter.scopes(ScopesArguments().apply {
                            frameId = it.id
                        }).join().scopes.asSequence().flatMap {
                            debugAdapter.variables(VariablesArguments().apply {
                                variablesReference = it.variablesReference
                            }).join().variables.asSequence()
                        }
                    }.toList()
                    // Then continue execution
                    debugAdapter.continue_(ContinueArguments().apply {
                        threadId = slot.captured.threadId
                    })
                }
                times++
            }
        }

        //When we run the program
        launch()

        //And wait for the debugging session to finish
        latch.await()

        // We expect the stopped event to have been triggered twice
        verify(exactly = 2) { client.stopped(any()) }

        // The state of the `local` variable to have been set correctly
        assertThat(locals.map { Pair(it.name, it.value) }, Matchers.hasItem(Pair("local", "123")))

        // The state of the `local` variable to have been set correctly
        val receiver = locals.find { it.name == "this" }

        assertThat(receiver, Matchers.not(Matchers.nullValue()))
    }

    @Test
    fun `Test classes should be added to JVM class-path`() {
        // Given we set an exception breakpoint
        debugAdapter.setExceptionBreakpoints(SetExceptionBreakpointsArguments().apply {
            filters = arrayOf("U")
        }).join()

        // And just continue whenever its hit
        client.continueOnBeak()

        // When we launch our debugging process with the `TestClassPath` variable set
        launch("-DTestClassPath=true")

        // And the process is finished
        latch.await()

        // We expect not to have hit a `java.lang.ClassNotFoundException`
        verify(exactly = 0) { client.stopped(any()) }
    }

    @Test
    fun `Type U ExceptionBreakpoints should hit on uncaught exceptions`() {
        // Given we set an exception breakpoint of type U
        debugAdapter.setExceptionBreakpoints(SetExceptionBreakpointsArguments().apply {
            filters = arrayOf("U")
        }).join()

        // And just continue whenever its hit
        client.continueOnBeak()

        // When we launch our debugging process with the `TestClassPath` variable set
        launch("-DTestExceptionBreakpoint=Uncaught")

        // And the process is finished
        latch.await()

        // We expect an exception breakpoint to be hit
        verify(exactly = 1) { client.stopped(withArg { assertThat(it.reason, equalTo("exception")) }) }
    }

    @Test
    fun `Type C ExceptionBreakpoints should hit on caught exceptions`() {
        // Given we set an exception breakpoint of type U
        debugAdapter.setExceptionBreakpoints(SetExceptionBreakpointsArguments().apply {
            filters = arrayOf("C")
        }).join()

        // And just continue whenever its hit
        client.continueOnBeak()

        // When we launch our debugging process with the `TestClassPath` variable set
        launch("-DTestExceptionBreakpoint=Caught")

        // And the process is finished
        latch.await()

        // We expect an exception breakpoint to be hit
        verify(exactly = 1) { client.stopped(withArg { assertThat(it.reason, equalTo("exception")) }) }

    }

    fun launch(vmArguments: String = this.defaultVmArguments) {
        println("Launching...")
        debugAdapter.launch(
            mapOf(
                "projectRoot" to absoluteWorkspaceRoot.toString(),
                "mainClass" to mainClass,
                "vmArguments" to vmArguments
            )
        ).join()
        println("Launched")
    }

    private fun IDebugProtocolClient.continueOnBeak() {
        client = this // Workaround to be able to call client from within mockk scope
        // And just continue whenever its hit
        slot<StoppedEventArguments>().also { slot ->
            every { client.stopped(capture(slot)) } answers {
                debugAdapter.continue_(ContinueArguments().apply {
                    threadId = slot.captured.threadId
                })
            }
        }
    }
}

