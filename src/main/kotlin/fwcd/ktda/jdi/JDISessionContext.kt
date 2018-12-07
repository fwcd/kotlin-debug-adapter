package fwcd.ktda.jdi

import fwcd.ktda.core.Position
import fwcd.ktda.jdi.event.VMEventBus
import com.sun.jdi.Location

interface JDISessionContext {
	val eventBus: VMEventBus
	val pendingStepRequestThreadIds: MutableSet<Long>
	
	fun positionOf(location: Location): Position?
}
