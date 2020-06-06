package org.javacs.ktda

import java.nio.file.Path
import java.nio.file.Paths
import org.eclipse.lsp4j.debug.DisconnectArguments
import org.javacs.ktda.adapter.KotlinDebugAdapter
import org.javacs.ktda.jdi.launch.JDILauncher
import org.junit.After

abstract class DebugAdapterTestFixture(relativeWorkspaceRoot: String, mainClass: String) {
    val absoluteWorkspaceRoot: Path = Paths.get(DebugAdapterTestFixture::class.java.getResource("/").toURI()).resolve(relativeWorkspaceRoot)
    val debugAdapter: KotlinDebugAdapter = JDILauncher()
        .let(::KotlinDebugAdapter)
    
    fun launch() {
        debugAdapter.launch(mapOf(
            "projectRoot" to absoluteWorkspaceRoot.toString(),
            "mainClass" to "sample.workspace.AppKt"
        ))
    }

    @After fun closeDebugAdapter() {
        debugAdapter.disconnect(DisconnectArguments()).join()
    }
}
