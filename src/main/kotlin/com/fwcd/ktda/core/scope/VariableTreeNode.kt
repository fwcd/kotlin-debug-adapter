package com.fwcd.ktda.core.scope

/**
 * A descriptor for a collection of child variables.
 * (usually a scope or a variable's fields)
 */
interface VariableTreeNode {
	val name: String
	val value: String
	val childs: List<VariableTreeNode>
}
