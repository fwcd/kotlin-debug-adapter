package org.javacs.ktda.core.scope

/**
 * A descriptor for a collection of child variables.
 * (usually a scope or a variable's fields)
 */
interface VariableTreeNode {
	val name: String
	val value: String?
		get() = null
	val type: String?
		get() = null
	val childs: List<VariableTreeNode>?
		get() = null
	val id: Long?
		get() = null
	
	// TODO: Setters for values?
}
