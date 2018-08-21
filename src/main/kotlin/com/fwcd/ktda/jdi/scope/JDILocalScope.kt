package com.fwcd.ktda.jdi.scope

import com.fwcd.ktda.core.scope.VariableTreeNode
import com.sun.jdi.AbsentInformationException

class JDILocalScope(
	frame: com.sun.jdi.StackFrame
): VariableTreeNode {
	override val name: String = "Locals"
	override val childs: List<VariableTreeNode> = try {
		frame.visibleVariables()
			.map { JDIVariable(it.name(), frame.getValue(it)) } +
			try {
				listOf(JDIVariable("this", frame.thisObject()))
			} catch (e: IllegalStateException) { emptyList<JDIVariable>() }
	} catch (e: AbsentInformationException) { emptyList() }
}
