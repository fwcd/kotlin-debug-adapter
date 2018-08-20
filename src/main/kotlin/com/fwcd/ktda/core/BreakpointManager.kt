package com.fwcd.ktda.core

interface BreakpointManager {
	fun setAllBreakpointsIn(source: Source, breakpoints: List<Breakpoint>)
}
