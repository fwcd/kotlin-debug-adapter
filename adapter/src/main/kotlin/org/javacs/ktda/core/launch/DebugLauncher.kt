package org.javacs.ktda.core.launch

import org.javacs.ktda.core.Debuggee
import org.javacs.ktda.core.DebugContext

interface DebugLauncher {
	fun launch(config: LaunchConfiguration, context: DebugContext): Debuggee
	
	fun attach(config: AttachConfiguration, context: DebugContext): Debuggee
}
