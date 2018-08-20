package com.fwcd.ktda.core.launch

import com.fwcd.ktda.core.Debuggee

interface DebugLauncher {
	fun launch(config: LaunchConfiguration): Debuggee
	
	fun attach(config: AttachConfiguration): Debuggee
}
