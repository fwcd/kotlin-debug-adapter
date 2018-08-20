package com.fwcd.ktda.jdi

import com.fwcd.ktda.core.scope.VariableTreeNode

class JDIVariable(
	frame: com.sun.jdi.StackFrame,
	variable: com.sun.jdi.LocalVariable
): VariableTreeNode {
	override val name: String = variable.name()
	override val value: String = frame.getValue(variable).toString() // TODO: Better string representation
	override val childs: List<VariableTreeNode> = emptyList() // TODO: Fields as child variables
}
