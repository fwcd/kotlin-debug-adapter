package org.javacs.ktda.jdi.stack

import org.javacs.ktda.core.stack.StackTrace
import org.javacs.ktda.core.stack.StackFrame
import org.javacs.ktda.jdi.JDISessionContext

class JDIStackTrace(
	jdiFrames: List<com.sun.jdi.StackFrame>,
	context: JDISessionContext
) : StackTrace {
	override val frames: List<StackFrame> = jdiFrames.map { JDIStackFrame(it, context) }
}
