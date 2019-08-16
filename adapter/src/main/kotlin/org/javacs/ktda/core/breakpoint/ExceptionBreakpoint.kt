package org.javacs.ktda.core.breakpoint

enum class ExceptionBreakpoint(
	val id: String,
	val label: String
) {
	CAUGHT("C", "Caught Exceptions"),
	UNCAUGHT("U", "Uncaught Exceptions")
}
