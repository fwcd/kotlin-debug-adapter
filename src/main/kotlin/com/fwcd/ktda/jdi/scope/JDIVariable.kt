package com.fwcd.ktda.jdi.scope

import com.fwcd.ktda.core.scope.VariableTreeNode
import com.fwcd.ktda.LOG
import com.sun.jdi.ReferenceType
import com.sun.jdi.LocalVariable
import com.sun.jdi.ArrayType
import com.sun.jdi.ArrayReference
import com.sun.jdi.ObjectReference
import com.sun.jdi.Value

class JDIVariable(
	override val name: String,
	jdiValue: Value
): VariableTreeNode {
	override val value: String = jdiValue.toString() // TODO: Better string representation
	override val type: String = jdiValue.type().name()
	override val childs: List<VariableTreeNode> by lazy { childrenOf(jdiValue) }
	
	private fun childrenOf(jdiValue: Value): List<VariableTreeNode> {
		val jdiType = jdiValue.type()
		// LOG.info("$name has type ${jdiType::class.simpleName}") // DEBUG
		return when (jdiType) {
			is ReferenceType -> when (jdiType) {
				is ArrayType -> arrayElementsOf(jdiValue as ArrayReference)
				else -> fieldsOf(jdiValue as ObjectReference, jdiType)
			}
			else -> emptyList()
		}
	}
	
	private fun arrayElementsOf(jdiValue: ArrayReference): List<VariableTreeNode> = jdiValue.values
		.mapIndexed { i, it -> JDIVariable(i.toString(), it) }
		
	private fun fieldsOf(jdiValue: ObjectReference, jdiType: ReferenceType) = jdiType.allFields()
		.map { JDIVariable(it.name(), jdiValue.getValue(it)) }
}
