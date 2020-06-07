package org.javacs.ktda.core.stack

import org.javacs.ktda.core.Position
import org.javacs.ktda.core.completion.CompletionItem
import org.javacs.ktda.core.scope.VariableTreeNode

interface StackFrame {
	val name: String
	val position: Position?
	val scopes: List<VariableTreeNode>

	fun evaluate(expression: String): VariableTreeNode?

	fun completions(expression: String): List<CompletionItem>
}
