package com.fwcd.ktda.core.breakpoint

import com.fwcd.ktda.core.Source
import com.fwcd.ktda.util.ListenerList

class BreakpointManager {
	private val breakpoints = mutableMapOf<Source, List<Breakpoint>>()
	val listeners = ListenerList<List<Breakpoint>>()
	
	/** Attempts to place breakpoints in a source and returns the successfully placed ones */
	fun setAllBreakpointsIn(source: Source, sourceBreakpoints: List<SourceBreakpoint>): List<Breakpoint> {
		val actualBreakpoints = sourceBreakpoints.mapNotNull { it.toActualBreakpoint() }
		breakpoints[source] = actualBreakpoints
		
		fireListeners()
		return actualBreakpoints
	}
	
	// TODO: Validation logic
	private fun SourceBreakpoint.toActualBreakpoint(): Breakpoint? = Breakpoint(position)
	
	private fun fireListeners() = listeners.fire(allBreakpoints())
	
	fun allBreakpoints() = breakpoints.values.flatten()
}
