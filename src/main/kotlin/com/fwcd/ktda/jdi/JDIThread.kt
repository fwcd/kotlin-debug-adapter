package com.fwcd.ktda.jdi

import com.fwcd.ktda.core.DebuggeeThread
import com.fwcd.ktda.core.stack.StackTrace
import com.sun.jdi.ThreadReference

class JDIThread(
	private val threadRef: ThreadReference
): DebuggeeThread {
	override fun pause() {
		if (!threadRef.isSuspended()) {
			threadRef.suspend()
		}
	}
	
	override fun resume() {
		(0 until threadRef.suspendCount()).forEach {
			threadRef.resume()
		}
	}
	
	override fun stackTrace() = TODO("Stack trace not implemented")
}
