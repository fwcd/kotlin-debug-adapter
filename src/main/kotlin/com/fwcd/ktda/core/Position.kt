package com.fwcd.ktda.core

/** A source code position */
class Position(
	val source: Source,
	val lineNumber: Int,
	val columnNumber: Int? = null
)
