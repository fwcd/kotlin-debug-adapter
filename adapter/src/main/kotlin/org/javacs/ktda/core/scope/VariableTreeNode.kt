package org.javacs.ktda.core.scope

import org.javacs.ktda.util.Identifiable

/**
 * A descriptor for a collection of child variables.
 * (usually a scope or a variable's fields)
 */
interface VariableTreeNode : Identifiable {
	val name: String
	val value: String?
		get() = null
	val type: String?
		get() = null
	val childs: List<VariableTreeNode>?
		get() = null
	
	// TODO: Setters for values?
}
