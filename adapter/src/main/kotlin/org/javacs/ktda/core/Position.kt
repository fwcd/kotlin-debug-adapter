package org.javacs.ktda.core

/** A source code position. Line and column numbers are 1-indexed */
class Position(
	val source: Source,
	val lineNumber: Int,
	val columnNumber: Int? = null
)
