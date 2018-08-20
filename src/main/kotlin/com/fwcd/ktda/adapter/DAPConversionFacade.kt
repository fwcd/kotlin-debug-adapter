package com.fwcd.ktda.adapter

import java.nio.file.Paths
import com.fwcd.ktda.core.Position

typealias DAPSource = org.eclipse.lsp4j.debug.Source
typealias DAPSourceBreakpoint = org.eclipse.lsp4j.debug.SourceBreakpoint
typealias DAPBreakpoint = org.eclipse.lsp4j.debug.Breakpoint
typealias DAPStackFrame = org.eclipse.lsp4j.debug.StackFrame
typealias InternalSource = com.fwcd.ktda.core.Source
typealias InternalSourceBreakpoint = com.fwcd.ktda.core.breakpoint.SourceBreakpoint
typealias InternalBreakpoint = com.fwcd.ktda.core.breakpoint.Breakpoint
typealias internalFrame = com.fwcd.ktda.core.stack.StackFrame

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
	
	fun toDAPStackFrame(internalFrame: internalFrame, frameId: Long) = DAPStackFrame().apply {
		id = frameId
		name = internalFrame.name
		line = internalFrame.position?.lineNumber?.let(lineConverter::toExternalLine) ?: 0L
		column = 0L
		source = internalFrame.position?.source?.let(::toDAPSource)
	}
}
