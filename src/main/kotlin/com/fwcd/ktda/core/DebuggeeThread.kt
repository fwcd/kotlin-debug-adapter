package com.fwcd.ktda.core

import com.fwcd.ktda.core.stack.StackTrace

interface DebuggeeThread {
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
