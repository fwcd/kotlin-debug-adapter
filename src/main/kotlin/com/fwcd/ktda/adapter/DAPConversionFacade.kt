package com.fwcd.ktda.adapter

import java.nio.file.Paths
import com.fwcd.ktda.core.Position

typealias DAPSource = org.eclipse.lsp4j.debug.Source
typealias DAPSourceBreakpoint = org.eclipse.lsp4j.debug.SourceBreakpoint
typealias DAPBreakpoint = org.eclipse.lsp4j.debug.Breakpoint
typealias InternalSource = com.fwcd.ktda.core.Source
typealias InternalSourceBreakpoint = com.fwcd.ktda.core.breakpoint.SourceBreakpoint
typealias InternalBreakpoint = com.fwcd.ktda.core.breakpoint.Breakpoint

/**
 * Handles conversions between debug adapter types
 * and internal types.
 */
class DAPConversionFacade(
	var lineConverter: LineNumberConverter = LineNumberConverter()
) {
	fun toInternalSource(dapSource: DAPSource) = InternalSource(
		name = dapSource.name,
		filePath = Paths.get(dapSource.path)
	)
	
	fun toDAPSource(internalSource: InternalSource) = DAPSource().apply {
		name = internalSource.name
		path = internalSource.filePath.toAbsolutePath().toString()
	}
	
	fun toInternalSourceBreakpoint(dapSource: DAPSource, dapSrcBreakpoint: DAPSourceBreakpoint) = InternalSourceBreakpoint(
		position = Position(
			source = toInternalSource(dapSource),
			lineNumber = lineConverter.toInternalLine(dapSrcBreakpoint.line)
		)
	)
	
	fun toDAPBreakpoint(internalBreakpoint: InternalBreakpoint) = DAPBreakpoint().apply {
		source = toDAPSource(internalBreakpoint.position.source)
		line = lineConverter.toExternalLine(internalBreakpoint.position.lineNumber)
		verified = true
	}
}
