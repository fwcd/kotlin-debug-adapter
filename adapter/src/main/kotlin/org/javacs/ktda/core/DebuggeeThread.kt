package org.javacs.ktda.core

import org.javacs.ktda.core.stack.StackTrace

interface DebuggeeThread {
	val name: String
	val id: Long
	
	/** Tries to pause the thread, returning whether the operation was successful or not */
	fun pause(): Boolean
	
	/** Tries to resume the thread, returning whether the operation was successful or not */
	fun resume(): Boolean
	
	fun stepOver()
	
	fun stepInto()
	
	fun stepOut()
	
	fun stackTrace(): StackTrace
}
