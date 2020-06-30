package org.javacs.ktda.core.event

import org.javacs.ktda.util.ListenerList

interface DebuggeeEventBus {
	val exitListeners: ListenerList<ExitEvent>
	val breakpointListeners: ListenerList<BreakpointStopEvent>
	val stepListeners: ListenerList<StepStopEvent>
	val exceptionListeners: ListenerList<ExceptionStopEvent>
	val threadListeners: ListenerList<ThreadEvent>
}
