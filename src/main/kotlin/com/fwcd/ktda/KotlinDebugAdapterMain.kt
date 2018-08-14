package com.fwcd.ktda

import com.fwcd.ktda.util.ExitingInputStream
import org.eclipse.lsp4j.debug.launch.DSPLauncher

fun main(args: Array<String>) {
	val debugAdapter = KotlinDebugAdapter()
	val input = ExitingInputStream(System.`in`)
	val output = System.out
	val launcher = DSPLauncher.createServerLauncher(debugAdapter, input, output)
	
	debugAdapter.connect(launcher.remoteProxy)
	launcher.startListening()
}
