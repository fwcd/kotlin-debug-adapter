package org.javacs.ktda.jdi.scope

import org.javacs.ktda.core.scope.VariableTreeNode
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.StackFrame

class JDILocalScope(
	frame: StackFrame
) : VariableTreeNode {
	override val name: String = "Locals"
	override val childs: List<VariableTreeNode> = variablesIn(frame)
	
	private fun variablesIn(frame: StackFrame) = try {
		listOfNotNull(thisIn(frame)) + localsIn(frame)
	} catch (e: AbsentInformationException) { emptyList<JDIVariable>() }
	
	private fun localsIn(frame: StackFrame) = frame.visibleVariables()
		.map { JDIVariable(it.name(), frame.getValue(it)) }
	
	private fun thisIn(frame: StackFrame) = try {
		JDIVariable("this", frame.thisObject())
	} catch (e: IllegalStateException) { null }
}
