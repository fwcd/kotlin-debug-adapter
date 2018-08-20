package com.fwcd.ktda.jdi

import com.fwcd.ktda.core.Position
import com.sun.jdi.Location

interface JDIConversionFacade {
	fun positionOf(location: Location): Position?
}
