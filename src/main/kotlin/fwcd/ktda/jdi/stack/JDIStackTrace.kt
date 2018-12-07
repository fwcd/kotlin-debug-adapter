package fwcd.ktda.jdi.stack

import fwcd.ktda.core.stack.StackTrace
import fwcd.ktda.core.stack.StackFrame
import fwcd.ktda.jdi.JDISessionContext

class JDIStackTrace(
	jdiFrames: List<com.sun.jdi.StackFrame>,
	context: JDISessionContext
): StackTrace {
	override val frames: List<StackFrame> = jdiFrames.map { JDIStackFrame(it, context) }
}
