package com.fwcd.ktda.jdi

import com.fwcd.ktda.core.Position
import com.fwcd.ktda.jdi.event.VMEventBus
import com.sun.jdi.Location

interface JDISessionContext {
	val eventBus: VMEventBus
	val pendingStepRequestThreadIds: MutableSet<Long>
	
	fun positionOf(location: Location): Position?
}
