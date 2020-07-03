package org.javacs.ktda.core.launch

import java.nio.file.Path

class LaunchConfiguration(
	val classpath: Set<Path>,
	val mainClass: String,
	val projectRoot: Path,
	val vmArguments: String = "",
	val cwd: Path = projectRoot,
	val envs: Map<String, String> = mapOf()
)
