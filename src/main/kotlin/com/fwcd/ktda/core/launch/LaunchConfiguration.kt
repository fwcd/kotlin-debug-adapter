package com.fwcd.ktda.core.launch

import java.nio.file.Path

class LaunchConfiguration(
	val classpath: Set<Path>,
	val mainClass: String,
	val rootPath: Path
) {
	val sourcesRoot = rootPath.resolve("src").resolve("main").resolve("kotlin")
}
