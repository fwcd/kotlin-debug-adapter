package org.javacs.ktda.core.scope

data class BasicVariableTreeNode(
    override val name: String,
    override val value: String? = null,
    override val type: String? = null,
    override val childs: List<VariableTreeNode>? = null
) : VariableTreeNode
