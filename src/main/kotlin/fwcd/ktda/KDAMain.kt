package fwcd.ktda

import java.util.concurrent.Executors
import fwcd.ktda.adapter.KotlinDebugAdapter
import fwcd.ktda.core.launch.DebugLauncher
import fwcd.ktda.jdi.launch.JDILauncher
import fwcd.ktda.util.ExitingInputStream
import fwcd.ktda.util.LoggingInputStream
import fwcd.ktda.util.LoggingOutputStream
import org.eclipse.lsp4j.debug.launch.DSPLauncher
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.TerminatedEventArguments

/** Enable logging of raw input JSON messages (if it is enabled in the user's debug configuration). */
private const val JSON_IN_LOGGING = true
private const val JSON_IN_LOGGING_BUFFER_LINES = false

/** Enable logging of raw output JSON messages (if it is enabled in the user's debug configuration). */
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
