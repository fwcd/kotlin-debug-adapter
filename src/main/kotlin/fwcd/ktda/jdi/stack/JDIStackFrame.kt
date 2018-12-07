package fwcd.ktda.jdi.stack

import fwcd.ktda.core.Position
import fwcd.ktda.core.scope.VariableTreeNode
import fwcd.ktda.core.stack.StackFrame
import fwcd.ktda.jdi.JDISessionContext
import fwcd.ktda.jdi.scope.JDILocalScope

class JDIStackFrame(
	frame: com.sun.jdi.StackFrame,
	context: JDISessionContext
): StackFrame {
	private val location = frame.location()
	override val name: String = location.method()?.name() ?: "Unknown"
	override val position: Position? = context.positionOf(location)
	override val scopes: List<VariableTreeNode> by lazy { listOf(
		JDILocalScope(frame)
	) }
	// TODO: Scope "Fields"
	// TODO: Argument values?
}
