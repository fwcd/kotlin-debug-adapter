package com.fwcd.ktda.core

interface DebugLauncher {
	fun launch(project: LaunchConfiguration): Debuggee
	
	// TODO: Support attaching to an existing JVM
}
