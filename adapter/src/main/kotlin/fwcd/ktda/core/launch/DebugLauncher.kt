package fwcd.ktda.core.launch

import fwcd.ktda.core.Debuggee
import fwcd.ktda.core.DebugContext

interface DebugLauncher {
	fun launch(config: LaunchConfiguration, context: DebugContext): Debuggee
	
	fun attach(config: AttachConfiguration, context: DebugContext): Debuggee
}
