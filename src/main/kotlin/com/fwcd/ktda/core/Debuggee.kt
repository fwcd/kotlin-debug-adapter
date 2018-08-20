package com.fwcd.ktda.core

import com.fwcd.ktda.core.event.DebuggeeEventBus
import com.fwcd.ktda.util.ObservableList

/** A debuggee that is launched upon construction */
interface Debuggee {
	val threads: ObservableList<DebuggeeThread>
	val eventBus: DebuggeeEventBus
	
	fun stop()
	
	fun threadByID(id: Long): DebuggeeThread? = threads
		.asSequence()
		.filter { it.id == id }
		.firstOrNull()
}
