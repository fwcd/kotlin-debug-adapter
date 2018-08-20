package com.fwcd.ktda.core

import com.fwcd.ktda.core.event.DebuggeeEventBus
import com.fwcd.ktda.util.ObservableList

/** A debuggee that is launched upon construction */
interface Debuggee {
	val threads: ObservableList<DebuggeeThread>
	val eventBus: DebuggeeEventBus
	
	fun stop()
	
	fun stepOver()
	
	fun stepInto()
	
	fun stepOut()
}
