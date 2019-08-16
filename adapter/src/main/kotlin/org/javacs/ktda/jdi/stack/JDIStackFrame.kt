package org.javacs.ktda.jdi.stack

import org.javacs.ktda.core.Position
import org.javacs.ktda.core.scope.VariableTreeNode
import org.javacs.ktda.core.stack.StackFrame
import org.javacs.ktda.jdi.JDISessionContext
import org.javacs.ktda.jdi.scope.JDILocalScope

class JDIStackFrame(
	frame: com.sun.jdi.StackFrame,
	context: JDISessionContext
) : StackFrame {
	private val location = frame.location()
	override val name: String = location.method()?.name() ?: "Unknown"
	override val position: Position? = context.positionOf(location)
	override val scopes: List<VariableTreeNode> by lazy { listOf(
		JDILocalScope(frame)
	) }
	// TODO: Scope "Fields"
	// TODO: Argument values?
}
