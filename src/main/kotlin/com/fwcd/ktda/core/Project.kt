package com.fwcd.ktda.core

import java.nio.file.Path

class Project(
	val classpath: Set<Path>,
	val mainClass: String,
	val rootPath: Path
) {
	val sourcesRoot = rootPath.resolve("src").resolve("main").resolve("kotlin")
}
