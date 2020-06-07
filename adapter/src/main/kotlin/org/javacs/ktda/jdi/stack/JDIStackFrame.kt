package org.javacs.ktda.jdi.stack

import org.javacs.kt.LOG
import org.javacs.ktda.core.Position
import org.javacs.ktda.core.completion.CompletionItem
import org.javacs.ktda.core.completion.CompletionItemType
import org.javacs.ktda.core.scope.VariableTreeNode
import org.javacs.ktda.core.stack.StackFrame
import org.javacs.ktda.jdi.JDISessionContext
import org.javacs.ktda.jdi.scope.JDILocalScope
import com.sun.jdi.InvalidStackFrameException

class JDIStackFrame(
	frame: com.sun.jdi.StackFrame,
	context: JDISessionContext
) : StackFrame {
	private val location = frame.location()
	override val name: String = location.method()?.name() ?: "Unknown"
	override val position: Position? = context.positionOf(location)
	override val scopes: List<VariableTreeNode> by lazy {
		try {
			listOf(JDILocalScope(frame))
		} catch (e: InvalidStackFrameException) {
			LOG.warn("Could not fetch scopes, invalid stack frame: {}", e.message)
			emptyList<VariableTreeNode>()
		}
	}

	private val variables by lazy { scopes.flatMap { it.childs ?: emptyList() } }

	// TODO: Scope "Fields"
	// TODO: Argument values?

	private fun evaluateQualified(qualName: List<String>, scopeVariables: List<VariableTreeNode> = variables): VariableTreeNode? =
		qualName.firstOrNull().let { qual ->
			val rest = qualName.drop(1)
			scopeVariables
				.filter { it.name == qual }
				.mapNotNull { if (rest.isEmpty()) it else evaluateQualified(rest, it.childs ?: emptyList()) }
				.firstOrNull()
		}

	private fun completeQualified(qualName: List<String>, scopeVariables: List<VariableTreeNode> = variables): List<CompletionItem> =
		qualName.firstOrNull()?.let { qual ->
			val rest = qualName.drop(1)
			scopeVariables
				.filter { it.name == qual }
				.flatMap { completeQualified(rest, it.childs ?: emptyList()) }
				.takeIf { it.isNotEmpty() }
				?: scopeVariables
					.takeIf { rest.isEmpty() }
					?.filter { it.name.startsWith(qual) }
					?.map { CompletionItem(it.name, CompletionItemType.VARIABLE) }
		}.orEmpty()

	private fun parseQualified(expression: String): List<String> = expression.split(".")

	override fun evaluate(expression: String): VariableTreeNode? {
		// TODO: Implement proper expression parsing
		//
		// Note that expression parsing is not part of the JDI
		// (see https://www.oracle.com/technetwork/java/javase/tech/faqs-jsp-142584.html#QV1)
		// There is a JDI-compatible expression parser in the JDPA examples.
		// Unfortunately, however, it is not exported by the jdk/jdi module
		// and as such cannot be imported:
		// 
		// com.sun.tools.example.debug.expr.ExpressionParser
		//
		// Creating JDI values from primitives and strings is possible though,
		// using VirtualMachine.mirrorOf.

		val qualified = parseQualified(expression)
		return evaluateQualified(qualified)
			?: evaluateQualified(listOf("this") + qualified)
	}

	override fun completions(expression: String): List<CompletionItem> {
		val qualified = parseQualified(expression)
		return completeQualified(qualified) + completeQualified(listOf("this") + qualified)
	}
}
