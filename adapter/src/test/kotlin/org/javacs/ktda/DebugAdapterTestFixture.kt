package org.javacs.ktda

import java.nio.file.Path
import java.nio.file.Paths
import org.eclipse.lsp4j.debug.DisconnectArguments
import org.javacs.ktda.adapter.KotlinDebugAdapter
import org.javacs.ktda.jdi.launch.JDILauncher
import org.junit.AfterClass

abstract class DebugAdapterTestFixture(relativeWorkspaceRoot: String) {
    val absoluteWorkspaceRoot: Path = Paths.get(DebugAdapterTestFixture::class.java.getResource("/").toURI()).resolve(relativeWorkspaceRoot)
    val debugAdapter: KotlinDebugAdapter = JDILauncher()
        .let(::KotlinDebugAdapter)
        .also { it.launch(mapOf(
            "projectRoot" to absoluteWorkspaceRoot.toString(),
            "mainClass" to "sample.workspace.AppKt"
        )) }

    @AfterClass private fun closeDebugAdapter() {
        debugAdapter.disconnect(DisconnectArguments()).join()
    }
}
