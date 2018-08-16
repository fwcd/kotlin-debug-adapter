package com.fwcd.ktda.jdi

import com.sun.jdi.event.Event
import com.sun.jdi.event.EventSet

class DebugEvent(
	private val jdiEvent: Event,
	private val jdiEventSet: EventSet
) {
	var resumeThreads = true
}
