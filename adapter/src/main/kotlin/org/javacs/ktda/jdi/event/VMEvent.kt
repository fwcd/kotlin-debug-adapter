package org.javacs.ktda.jdi.event

import com.sun.jdi.event.Event
import com.sun.jdi.event.EventSet

class VMEvent<E: Event>(
	val jdiEvent: E,
	val jdiEventSet: EventSet
) {
	var resumeThreads = true
}
