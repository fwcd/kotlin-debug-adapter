package fwcd.ktda.core.event

import fwcd.ktda.util.ListenerList

interface DebuggeeEventBus {
	val exitListeners: ListenerList<ExitEvent>
	val breakpointListeners: ListenerList<BreakpointStopEvent>
	val stepListeners: ListenerList<StepStopEvent>
	var exceptionListeners: ListenerList<ExceptionStopEvent>
}
