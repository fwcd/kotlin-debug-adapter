package com.fwcd.ktda

import java.util.concurrent.Executors
import com.fwcd.ktda.adapter.KotlinDebugAdapter
import com.fwcd.ktda.core.launch.DebugLauncher
import com.fwcd.ktda.jdi.launch.JDILauncher
import com.fwcd.ktda.util.ExitingInputStream
import com.fwcd.ktda.util.LoggingInputStream
import com.fwcd.ktda.util.LoggingOutputStream
import org.eclipse.lsp4j.debug.launch.DSPLauncher
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.TerminatedEventArguments

/** Logs the raw JSON messages from stdin (useful for debugging) */
private const val JSON_IN_LOGGING = false
private const val JSON_IN_LOGGING_BUFFER_LINES = false

/** Logs the raw JSON messages from stdout (useful for debugging) */
private const val JSON_OUT_LOGGING = false
private const val JSON_OUT_LOGGING_BUFFER_LINES = false

fun main(args: Array<String>) {
	LOG.connectJULFrontend()
	
	val launcher: DebugLauncher = JDILauncher()
	var client: IDebugProtocolClient? = null
	
	// Setup IO streams for JSON communication
	
	val input = LoggingInputStream(ExitingInputStream(System.`in`) {
		client?.terminated(TerminatedEventArguments())
		System.exit(0)
	}, JSON_IN_LOGGING, JSON_IN_LOGGING_BUFFER_LINES)
	val output = LoggingOutputStream(System.out, JSON_OUT_LOGGING, JSON_OUT_LOGGING_BUFFER_LINES)

	// Create debug adapter and launcher
	
	val debugAdapter = KotlinDebugAdapter(launcher)
	val threads = Executors.newSingleThreadExecutor { Thread(it, "server") }
	val serverLauncher = DSPLauncher.createServerLauncher(debugAdapter, input, output, threads) { it }
	
	debugAdapter.connect(serverLauncher.remoteProxy)
	serverLauncher.startListening()
}
