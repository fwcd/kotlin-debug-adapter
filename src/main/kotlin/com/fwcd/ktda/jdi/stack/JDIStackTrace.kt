package com.fwcd.ktda.jdi.stack

import com.fwcd.ktda.core.stack.StackTrace
import com.fwcd.ktda.core.stack.StackFrame
import com.fwcd.ktda.jdi.JDIConversionFacade

class JDIStackTrace(
	jdiFrames: List<com.sun.jdi.StackFrame>,
	converter: JDIConversionFacade
): StackTrace {
	override val frames: List<StackFrame> = jdiFrames.map { JDIStackFrame(it, converter) }
}
