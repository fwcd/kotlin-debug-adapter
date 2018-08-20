package com.fwcd.ktda.adapter

import java.nio.file.Paths
import com.fwcd.ktda.util.ListenerList
import org.eclipse.lsp4j.debug.SourceBreakpoint
import org.eclipse.lsp4j.debug.Breakpoint
import org.eclipse.lsp4j.debug.Source
import com.sun.jdi.Location

class BreakpointManager {
	private val breakpoints = mutableMapOf<Source, List<Breakpoint>>()
	val listeners = ListenerList<List<Breakpoint>>()
	var lineOffset = 0 // JDI line number + lineOffset = DAP line number
	
	fun setAllInSource(source: Source, srcBreakpoints: Array<out SourceBreakpoint>): List<Breakpoint> {
		val convertedBreakpoints = srcBreakpoints
			.map { toBreakpoint(source, it) }
		breakpoints[source] = convertedBreakpoints
		
		fireListeners()
		return convertedBreakpoints
	}
	
	fun breakpointAt(location: Location) = breakpoints
		.filterKeys { Paths.get(it.path) == Paths.get(location.sourcePath()) }
		.values
		.flatten()
		.filter { it.line.toInt() == (location.lineNumber() + lineOffset) }
		.firstOrNull()
	
	private fun fireListeners() = listeners.fire(allBreakpoints())
	
	private fun toBreakpoint(source: Source, srcBreakpoint: SourceBreakpoint) =
		Breakpoint().also {
			it.source = source
			it.verified = true
			it.line = srcBreakpoint.line
			it.column = srcBreakpoint.column
		}
	
	fun allBreakpoints(): List<Breakpoint> = breakpoints.values.flatten()
}
