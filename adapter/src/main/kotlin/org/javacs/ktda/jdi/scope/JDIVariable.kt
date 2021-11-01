package org.javacs.ktda.jdi.scope

import org.javacs.ktda.core.scope.VariableTreeNode
import com.sun.jdi.ReferenceType
import com.sun.jdi.LocalVariable
import com.sun.jdi.ArrayType
import com.sun.jdi.ArrayReference
import com.sun.jdi.ObjectReference
import com.sun.jdi.Value
import com.sun.jdi.Type

class JDIVariable(
	override val name: String,
	private val jdiValue: Value?,
	jdiType: Type? = null
) : VariableTreeNode {
	override val value: String = jdiValue?.toString() ?: "null" // TODO: Better string representation
	override val type: String = (jdiType?.name() ?: jdiValue?.type()?.name()) ?: "Unknown type"
	override val childs: List<VariableTreeNode>? by lazy { jdiValue?.let(::childrenOf) }
	override val id: Long? = (jdiValue as? ObjectReference)?.uniqueID() ?: (jdiValue as? ArrayReference)?.uniqueID()
	
	private fun childrenOf(jdiValue: Value): List<VariableTreeNode> {
		val jdiType = jdiValue.type()
		// LOG.info("$name has type {}", jdiType::class.simpleName) // DEBUG
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
		.map { JDIVariable(it.name(), jdiValue.getValue(it), jdiType) }
}
