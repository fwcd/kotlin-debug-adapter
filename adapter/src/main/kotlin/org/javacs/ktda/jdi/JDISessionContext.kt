package org.javacs.ktda.jdi

import org.javacs.ktda.core.Position
import org.javacs.ktda.jdi.event.VMEventBus
import com.sun.jdi.Location

interface JDISessionContext {
	val eventBus: VMEventBus
	val pendingStepRequestThreadIds: MutableSet<Long>
	
	fun positionOf(location: Location): Position?
}
