package org.javacs.ktda.adapter

import java.nio.file.Paths
import org.javacs.ktda.core.Position
import org.javacs.ktda.core.DebuggeeThread
import org.javacs.ktda.core.scope.VariableTreeNode
import org.javacs.ktda.util.ObjectPool
import org.javacs.ktda.util.KotlinDAException

private typealias DAPSource = org.eclipse.lsp4j.debug.Source
private typealias DAPSourceBreakpoint = org.eclipse.lsp4j.debug.SourceBreakpoint
private typealias DAPBreakpoint = org.eclipse.lsp4j.debug.Breakpoint
private typealias DAPStackFrame = org.eclipse.lsp4j.debug.StackFrame
private typealias DAPScope = org.eclipse.lsp4j.debug.Scope
private typealias DAPVariable = org.eclipse.lsp4j.debug.Variable
private typealias DAPThread = org.eclipse.lsp4j.debug.Thread
private typealias DAPExceptionBreakpointsFilter = org.eclipse.lsp4j.debug.ExceptionBreakpointsFilter
private typealias InternalSource = org.javacs.ktda.core.Source
private typealias InternalSourceBreakpoint = org.javacs.ktda.core.breakpoint.SourceBreakpoint
private typealias InternalExceptionBreakpoint = org.javacs.ktda.core.breakpoint.ExceptionBreakpoint
private typealias InternalBreakpoint = org.javacs.ktda.core.breakpoint.Breakpoint
private typealias InternalStackFrame = org.javacs.ktda.core.stack.StackFrame

/**
 * Handles conversions between debug adapter types
 * and internal types. This includes caching values
 * using ObjectPools and ids.
 */
class DAPConverter(
	var lineConverter: LineNumberConverter = LineNumberConverter()
) {
	val stackFramePool = ObjectPool<Long, InternalStackFrame>() // Contains stack frames owned by thread ids
	val variablesPool = ObjectPool<Unit, VariableTreeNode>() // Contains unowned variable trees (the ids are used as 'variables references')
	
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
	
	fun toInternalExceptionBreakpoint(filterID: String) = InternalExceptionBreakpoint
		.values()
		.find { it.id == filterID }
		?: throw KotlinDAException("${filterID} is not a valid exception breakpoint")
	
	fun toDAPExceptionBreakpointsFilter(internalBreakpoint: InternalExceptionBreakpoint) = DAPExceptionBreakpointsFilter().apply {
		filter = internalBreakpoint.id
		label = internalBreakpoint.label
		default_ = false
	}
	
	fun toDAPBreakpoint(internalBreakpoint: InternalBreakpoint) = DAPBreakpoint().apply {
		source = toDAPSource(internalBreakpoint.position.source)
		line = lineConverter.toExternalLine(internalBreakpoint.position.lineNumber)
		verified = true
	}
	
	fun toInternalStackFrame(frameId: Long) = stackFramePool.getByID(frameId)
	
	fun toDAPStackFrame(internalFrame: InternalStackFrame, threadId: Long) = DAPStackFrame().apply {
		id = stackFramePool.store(threadId, internalFrame)
		name = internalFrame.name
		line = internalFrame.position?.lineNumber?.let(lineConverter::toExternalLine) ?: 0L
		column = 0L
		source = internalFrame.position?.source?.let(::toDAPSource)
	}
	
	fun toDAPScope(variableTree: VariableTreeNode) = DAPScope().apply {
		name = variableTree.name
		variablesReference = variablesPool.store(Unit, variableTree)
		expensive = false
	}
	
	fun toVariableTree(variablesReference: Long) = variablesPool.getByID(variablesReference)
	
	fun toDAPVariable(variableTree: VariableTreeNode) = DAPVariable().apply {
		name = variableTree.name
		value = variableTree.value
		type = variableTree.type
		variablesReference = variableTree.childs?.takeIf { it.isNotEmpty() }?.let { variablesPool.store(Unit, variableTree) } ?: 0
	}
	
	fun toDAPThread(internalThread: DebuggeeThread) = DAPThread().apply {
		name = internalThread.name
		id = internalThread.id
	}
}
