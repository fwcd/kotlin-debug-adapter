package org.javacs.ktda

import java.nio.file.Path
import java.nio.file.Paths
import org.eclipse.lsp4j.debug.DisconnectArguments
import org.eclipse.lsp4j.debug.OutputEventArguments
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.javacs.ktda.adapter.KotlinDebugAdapter
import org.javacs.ktda.jdi.launch.JDILauncher
import org.junit.After

abstract class DebugAdapterTestFixture(relativeWorkspaceRoot: String, mainClass: String) : IDebugProtocolClient {
    val absoluteWorkspaceRoot: Path = Paths.get(DebugAdapterTestFixture::class.java.getResource("/").toURI()).resolve(relativeWorkspaceRoot)
    val debugAdapter: KotlinDebugAdapter = JDILauncher()
        .let(::KotlinDebugAdapter)
        .also { it.connect(this) }
    
    fun launch() {
        debugAdapter.launch(mapOf(
            "projectRoot" to absoluteWorkspaceRoot.toString(),
            "mainClass" to "sample.workspace.AppKt"
        ))
    }

    @After fun closeDebugAdapter() {
        debugAdapter.disconnect(DisconnectArguments()).join()
    }

    override fun output(args: OutputEventArguments) {
        println(args.output)
    }
}
