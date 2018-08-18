package com.fwcd.ktda.jdi

import com.sun.jdi.event.Event
import com.sun.jdi.event.EventSet

class DebugEvent<E: Event>(
	val jdiEvent: E,
	val jdiEventSet: EventSet
) {
	var resumeThreads = true
}
