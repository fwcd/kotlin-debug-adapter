package org.javacs.ktda.jdi.stack

import org.javacs.ktda.core.Position
import org.javacs.ktda.core.scope.VariableTreeNode
import org.javacs.ktda.core.stack.StackFrame
import org.javacs.ktda.jdi.JDISessionContext
import org.javacs.ktda.jdi.scope.JDILocalScope

class JDIStackFrame(
	frame: com.sun.jdi.StackFrame,
	context: JDISessionContext
) : StackFrame {
	private val location = frame.location()
	override val name: String = location.method()?.name() ?: "Unknown"
	override val position: Position? = context.positionOf(location)
	override val scopes: List<VariableTreeNode> by lazy { listOf(
		JDILocalScope(frame)
	) }

	// TODO: Scope "Fields"
	// TODO: Argument values?

	private fun evaluateQualified(qualName: List<String>, variables: List<VariableTreeNode> = scopes.flatMap { it.childs ?: emptyList() }): VariableTreeNode? =
		qualName.firstOrNull().let { qual ->
			val rest = qualName.drop(1)
			variables
				.filter { it.name == qual }
				.mapNotNull { if (rest.isEmpty()) it else evaluateQualified(rest, it.childs ?: emptyList()) }
				.firstOrNull()
		}

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

		val qualified = expression.split(".")
		return evaluateQualified(qualified)
			?: evaluateQualified(listOf("this") + qualified)
	}
}
