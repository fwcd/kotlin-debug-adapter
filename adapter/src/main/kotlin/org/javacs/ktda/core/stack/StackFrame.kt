package org.javacs.ktda.core.stack

import org.javacs.ktda.core.Position
import org.javacs.ktda.core.scope.VariableTreeNode

interface StackFrame {
	val name: String
	val position: Position?
	val scopes: List<VariableTreeNode>
}
