package com.fwcd.ktda.core.launch

import com.fwcd.ktda.core.Debuggee
import com.fwcd.ktda.core.DebugContext

interface DebugLauncher {
	fun launch(config: LaunchConfiguration, context: DebugContext): Debuggee
	
	fun attach(config: AttachConfiguration, context: DebugContext): Debuggee
}
