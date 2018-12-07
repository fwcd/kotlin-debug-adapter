package fwcd.ktda.core.stack

import fwcd.ktda.core.Position
import fwcd.ktda.core.scope.VariableTreeNode

interface StackFrame {
	val name: String
	val position: Position?
	val scopes: List<VariableTreeNode>
}
