package org.javacs.ktda.core

import java.nio.file.Path

/** A source unit descriptor (usually a file) */
data class Source(
	val name: String,
	val filePath: Path
)
