package com.fwcd.ktda.core.breakpoint

import com.fwcd.ktda.core.Source
import com.fwcd.ktda.util.ObservableSet
import com.fwcd.ktda.util.ObservableMap

class BreakpointManager {
	val breakpoints = ObservableMap<Source, List<Breakpoint>>()
	val exceptionBreakpoints = ObservableSet<ExceptionBreakpoint>()
	
	/** Attempts to place breakpoints in a source and returns the successfully placed ones */
	fun setAllIn(source: Source, sourceBreakpoints: List<SourceBreakpoint>): List<Breakpoint> {
		val actualBreakpoints = sourceBreakpoints.mapNotNull { it.toActualBreakpoint() }
		breakpoints[source] = actualBreakpoints
		return actualBreakpoints
	}
	
	// TODO: Validation logic
	private fun SourceBreakpoint.toActualBreakpoint(): Breakpoint? = Breakpoint(position)
}
