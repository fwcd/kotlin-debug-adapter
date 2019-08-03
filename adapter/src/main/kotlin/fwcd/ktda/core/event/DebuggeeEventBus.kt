package fwcd.ktda.core.event

import fwcd.ktda.util.ListenerList

interface DebuggeeEventBus {
	val stopListeners: ListenerList<StopEvent>
	val breakpointListeners: ListenerList<BreakpointPauseEvent>
	val stepListeners: ListenerList<StepPauseEvent>
	var exceptionListeners: ListenerList<ExceptionPauseEvent>
}
