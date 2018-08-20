package com.fwcd.ktda.jdi.stack

import com.fwcd.ktda.core.stack.StackTrace
import com.fwcd.ktda.core.stack.StackFrame
import com.fwcd.ktda.jdi.JDISessionContext

class JDIStackTrace(
	jdiFrames: List<com.sun.jdi.StackFrame>,
	context: JDISessionContext
): StackTrace {
	override val frames: List<StackFrame> = jdiFrames.map { JDIStackFrame(it, context) }
}
