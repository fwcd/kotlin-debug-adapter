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

abstract class DebugAdapterTestFixture(
    relativeWorkspaceRoot: String,
    private val mainClass: String
) : IDebugProtocolClient {
    val absoluteWorkspaceRoot: Path = Paths.get(DebugAdapterTestFixture::class.java.getResource("/Anchor.txt").toURI()).parent.resolve(relativeWorkspaceRoot)
    val debugAdapter: KotlinDebugAdapter = JDILauncher()
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
        }
    
    fun launch() {
        println("Launching...")
        debugAdapter.launch(mapOf(
            "projectRoot" to absoluteWorkspaceRoot.toString(),
            "mainClass" to mainClass
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
