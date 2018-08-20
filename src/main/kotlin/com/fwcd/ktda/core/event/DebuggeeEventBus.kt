package com.fwcd.ktda.core.event

import com.fwcd.ktda.util.ListenerList

interface DebuggeeEventBus {
	val stopListeners: ListenerList<StopEvent>
	val breakpointListeners: ListenerList<BreakpointEvent>
	val stepListeners: ListenerList<StepEvent>
}
