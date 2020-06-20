package org.javacs.ktda.core

import java.nio.file.Path

/** A source unit descriptor (usually a file) */
class Source(
	val name: String,
	val filePath: Path
) {
	override fun equals(other: Any?): Boolean {
		return (other is Source) && (name == other.name) && (filePath == other.filePath)
	}
}