package com.fwcd.ktda

import java.util.concurrent.Executors
import com.fwcd.ktda.util.ExitingInputStream
import org.eclipse.lsp4j.debug.launch.DSPLauncher
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient
import org.eclipse.lsp4j.debug.TerminatedEventArguments

fun main(args: Array<String>) {
	val debugAdapter = KotlinDebugAdapter()
	var client: IDebugProtocolClient? = null
	val threads = Executors.newSingleThreadExecutor { Thread(it, "server") }
	val input = ExitingInputStream(System.`in`) {
		client?.terminated(TerminatedEventArguments())
		System.exit(0)
	}
	val output = System.out
	val launcher = DSPLauncher.createServerLauncher(debugAdapter, input, output, threads) { it }
	
	debugAdapter.connect(launcher.remoteProxy)
	launcher.startListening()
}
