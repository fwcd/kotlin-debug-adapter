package com.fwcd.ktda.jdi.stack

import com.fwcd.ktda.core.Position
import com.fwcd.ktda.core.scope.VariableTreeNode
import com.fwcd.ktda.core.stack.StackFrame
import com.fwcd.ktda.jdi.JDIConversionFacade
import com.fwcd.ktda.jdi.scope.JDILocalScope

class JDIStackFrame(
	frame: com.sun.jdi.StackFrame,
	converter: JDIConversionFacade
): StackFrame {
	private val location = frame.location()
	override val name: String = location.method()?.name() ?: "Unknown"
	override val position: Position = converter.positionOf(location)
	override val scopes: List<VariableTreeNode> by lazy { listOf(
		JDILocalScope(frame)
	) }
	// TODO: Scope "Fields"
	// TODO: Argument values?
}
