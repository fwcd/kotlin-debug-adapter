package com.fwcd.ktda.jdi.scope

import com.fwcd.ktda.core.scope.VariableTreeNode
import com.sun.jdi.AbsentInformationException

class JDILocalScope(
	frame: com.sun.jdi.StackFrame
): VariableTreeNode {
	override val name: String = "Locals"
	override val value: String = ""
	override val childs: List<VariableTreeNode> = try {
		frame.visibleVariables().map { JDIVariable(frame, it) }
	} catch (e: AbsentInformationException) { emptyList() }
}
