package org.javacs.ktda

import java.nio.file.Path
import java.nio.file.Paths
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments
import org.eclipse.lsp4j.debug.InitializeRequestArguments
import org.eclipse.lsp4j.debug.DisconnectArguments
import org.eclipse.lsp4j.debug.OutputEventArguments
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.javacs.ktda.adapter.KotlinDebugAdapter
import org.javacs.ktda.jdi.launch.JDILauncher
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.hamcrest.Matchers.equalTo

abstract class DebugAdapterTestFixture(
    relativeWorkspaceRoot: String,
    private val mainClass: String,
    private val vmArguments: String = "",
    private val cwd: String = "",
    private val envs: Map<String, String> = mapOf()
) : IDebugProtocolClient {
    val absoluteWorkspaceRoot: Path = Paths.get(DebugAdapterTestFixture::class.java.getResource("/Anchor.txt").toURI()).parent.resolve(relativeWorkspaceRoot)
    lateinit var debugAdapter: KotlinDebugAdapter
    
    @Before fun startDebugAdapter() {
        // Build the project first
        val process = ProcessBuilder("./gradlew", "assemble")
            .directory(absoluteWorkspaceRoot.toFile())
            .inheritIO()
            .start()
        process.waitFor()
        assertThat(process.exitValue(), equalTo(0))
        
        debugAdapter = JDILauncher()
            .let(::KotlinDebugAdapter)
            .also {
                it.connect(this)
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
    }
    
    fun launch() {
        println("Launching...")
        debugAdapter.launch(mapOf(
            "projectRoot" to absoluteWorkspaceRoot.toString(),
            "mainClass" to mainClass,
            "vmArguments" to vmArguments,
            "cwd" to cwd,
            "envs" to envs
        )).join()
        println("Launched")
    }

    @After fun closeDebugAdapter() {
        debugAdapter.disconnect(DisconnectArguments()).join()
    }

    override fun output(args: OutputEventArguments) {
        println(args.output)
    }
}
