package com.fwcd.ktda.core.stack

import com.fwcd.ktda.core.Source
import com.fwcd.ktda.core.scope.VariableTreeNode

interface StackFrame {
	val name: String
	val source: Source?
	val scopes: List<VariableTreeNode>
}
