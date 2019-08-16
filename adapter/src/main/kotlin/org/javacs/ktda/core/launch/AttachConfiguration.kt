package org.javacs.ktda.core.launch

import java.nio.file.Path

class AttachConfiguration(
	val projectRoot: Path,
	val hostName: String,
	val port: Int,
	val timeout: Int
)
