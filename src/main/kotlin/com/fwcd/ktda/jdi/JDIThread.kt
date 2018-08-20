package com.fwcd.ktda.jdi

import com.fwcd.ktda.core.DebuggeeThread
import com.fwcd.ktda.core.stack.StackTrace
import com.fwcd.ktda.jdi.stack.JDIStackTrace
import com.fwcd.ktda.jdi.JDIConversionFacade
import com.sun.jdi.ThreadReference

class JDIThread(
	private val threadRef: ThreadReference,
	private val converter: JDIConversionFacade
): DebuggeeThread {
	override val id: Long = threadRef.uniqueID()
	
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
	
	override fun stackTrace() = JDIStackTrace(threadRef.frames(), converter)
}
