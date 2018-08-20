package com.fwcd.ktda.core.stack

import com.fwcd.ktda.core.Position
import com.fwcd.ktda.core.scope.VariableTreeNode

interface StackFrame {
	val name: String
	val position: Position?
	val scopes: List<VariableTreeNode>
}
