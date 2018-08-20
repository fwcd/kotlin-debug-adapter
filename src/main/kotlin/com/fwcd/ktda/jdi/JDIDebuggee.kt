package com.fwcd.ktda.jdi

import com.fwcd.ktda.core.Debuggee
import com.fwcd.ktda.core.DebuggeeThread
import com.fwcd.ktda.core.DebugContext
import com.fwcd.ktda.core.Position
import com.fwcd.ktda.core.Source
import com.fwcd.ktda.core.launch.LaunchConfiguration
import com.fwcd.ktda.core.event.DebuggeeEventBus
import com.fwcd.ktda.util.ObservableList
import com.fwcd.ktda.classpath.findValidKtFilePath
import com.sun.jdi.Location

class JDIDebuggee(
	private val config: LaunchConfiguration,
	private val context: DebugContext
): Debuggee, JDIConversionFacade {
	override val threads = ObservableList<DebuggeeThread>()
	override val eventBus: DebuggeeEventBus
		get() = TODO("JDIDebuggee not implemented")
	
	override fun start() = TODO("JDIDebuggee not implemented")
	
	override fun stop() = TODO("JDIDebuggee not implemented")
	
	override fun stepOver() = TODO("JDIDebuggee not implemented")
	
	override fun stepInto() = TODO("JDIDebuggee not implemented")
	
	override fun stepOut() = TODO("JDIDebuggee not implemented")
	
	override fun positionOf(location: Location): Position? = extractSource(location)
		?.let { Position(it, location.lineNumber()) }
	
	private fun extractSource(location: Location): Source? = location.sourcePath()
		?.let(config.sourcesRoot::resolve)
		?.let(::findValidKtFilePath)
		?.let(::Source)
}
