package org.javacs.ktda

import java.util.concurrent.Executors
import org.javacs.kt.LOG
import org.javacs.kt.util.ExitingInputStream
import org.javacs.ktda.adapter.KotlinDebugAdapter
import org.javacs.ktda.core.launch.DebugLauncher
import org.javacs.ktda.jdi.launch.JDILauncher
import org.javacs.ktda.util.LoggingInputStream
import org.javacs.ktda.util.LoggingOutputStream
import org.eclipse.lsp4j.debug.launch.DSPLauncher
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.TerminatedEventArguments

/** Enable logging of raw input JSON messages (if it is enabled in the user's debug configuration). */
private const val JSON_IN_LOGGING = true
private const val JSON_IN_LOGGING_BUFFER_LINES = true

/** Enable logging of raw output JSON messages (if it is enabled in the user's debug configuration). */
private const val JSON_OUT_LOGGING = true
private const val JSON_OUT_LOGGING_BUFFER_LINES = true

fun main(args: Array<String>) {
	LOG.connectJULFrontend()
	
	val launcher: DebugLauncher = JDILauncher()
	
	// Setup IO streams for JSON communication
	
	val input = LoggingInputStream(ExitingInputStream(System.`in`), JSON_IN_LOGGING, JSON_IN_LOGGING_BUFFER_LINES)
	val output = LoggingOutputStream(System.out, JSON_OUT_LOGGING, JSON_OUT_LOGGING_BUFFER_LINES)
	
	// Create debug adapter and launcher
	
	val debugAdapter = KotlinDebugAdapter(launcher)
	val threads = Executors.newSingleThreadExecutor { Thread(it, "server") }
	val serverLauncher = DSPLauncher.createServerLauncher(debugAdapter, input, output, threads) { it }
	
	debugAdapter.connect(serverLauncher.remoteProxy)
	serverLauncher.startListening()
}
