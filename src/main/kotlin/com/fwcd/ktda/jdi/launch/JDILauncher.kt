package com.fwcd.ktda.jdi.launch

import com.fwcd.ktda.core.launch.DebugLauncher
import com.fwcd.ktda.core.launch.LaunchConfiguration
import com.fwcd.ktda.core.launch.AttachConfiguration
import com.fwcd.ktda.core.Debuggee
import com.fwcd.ktda.core.DebugContext
import com.fwcd.ktda.jdi.JDIDebuggee

class JDILauncher: DebugLauncher {
	override fun launch(config: LaunchConfiguration, context: DebugContext) = JDIDebuggee(config, context)
	
	override fun attach(config: AttachConfiguration, context: DebugContext) = TODO("Attach not implemented")
}
