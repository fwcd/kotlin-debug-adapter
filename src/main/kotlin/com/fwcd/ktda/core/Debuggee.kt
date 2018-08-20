package com.fwcd.ktda.core

import com.fwcd.ktda.core.event.DebuggeeEventBus
import com.fwcd.ktda.util.ObservableList
import java.io.InputStream
import java.io.OutputStream

/** A debuggee that is launched upon construction */
interface Debuggee {
	val threads: ObservableList<DebuggeeThread>
	val eventBus: DebuggeeEventBus
	val stdin: OutputStream?
		get() = null
	val stdout: InputStream?
		get() = null
	val stderr: InputStream?
		get() = null
	
	fun stop()
	
	fun threadByID(id: Long): DebuggeeThread? = threads
		.asSequence()
		.filter { it.id == id }
		.firstOrNull()
}
