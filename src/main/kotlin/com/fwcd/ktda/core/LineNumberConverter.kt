package com.fwcd.ktda.core

/**
 * Converts between external and internal line numbering.
 * For example:
 *
 * An "external" (debug adapter) line number could be zero-indexed and
 * an "internal" (core) line number would be one-indexed.
 *
 * In this case, externalLineOffset would be -1.
 */
class LineNumberConverter(
	private val externalLineOffset: Int // Internal line + externalLineOffset = External line
) {
	fun toInternalLine(lineNumber: Int) = lineNumber - externalLineOffset
	
	fun toExternalLine(lineNumber: Int) = lineNumber + externalLineOffset
}
