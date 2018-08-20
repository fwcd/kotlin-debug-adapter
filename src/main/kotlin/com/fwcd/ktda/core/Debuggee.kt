package com.fwcd.ktda.core

import com.fwcd.ktda.core.event.DebuggeeEventBus
import com.fwcd.ktda.util.ObservableList

interface Debuggee {
	val threads: ObservableList<DebuggeeThread>
	val breakpointManager: BreakpointManager
	val eventBus: DebuggeeEventBus
	
	fun start()
	
	fun stop()
	
	fun stepOver()
	
	fun stepInto()
	
	fun stepOut()
}
