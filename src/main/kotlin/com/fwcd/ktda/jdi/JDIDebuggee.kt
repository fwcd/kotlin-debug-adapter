package com.fwcd.ktda.jdi

import com.fwcd.ktda.core.Debuggee
import com.fwcd.ktda.core.DebuggeeThread
import com.fwcd.ktda.core.DebugContext
import com.fwcd.ktda.core.launch.LaunchConfiguration
import com.fwcd.ktda.core.event.DebuggeeEventBus
import com.fwcd.ktda.util.ObservableList

class JDIDebuggee(
	private val config: LaunchConfiguration,
	private val context: DebugContext
): Debuggee {
	override val threads = ObservableList<DebuggeeThread>()
	override val eventBus: DebuggeeEventBus
		get() = TODO("JDIDebuggee not implemented")
	
	override fun start() = TODO("JDIDebuggee not implemented")
	
	override fun stop() = TODO("JDIDebuggee not implemented")
	
	override fun stepOver() = TODO("JDIDebuggee not implemented")
	
	override fun stepInto() = TODO("JDIDebuggee not implemented")
	
	override fun stepOut() = TODO("JDIDebuggee not implemented")
}
