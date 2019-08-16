package org.javacs.ktda.classpath

import java.nio.file.Path
import java.nio.file.Files
import org.javacs.kt.classpath.ClassPathResolver

/** Resolver for the project's own (compiled) class files. */
internal class ProjectClassesResolver(private val projectRoot: Path) : ClassPathResolver {
	override val resolverType: String = "Project classes"
	override val classpath: Set<Path> get() = sequenceOf(
		// Gradle
		sequenceOf("kotlin", "java").flatMap { language ->
			sequenceOf("main", "test").map { sourceSet ->
				resolveIfExists(projectRoot, "build", "classes", language, sourceSet)
			}
		},
		// Maven
		sequenceOf(resolveIfExists(projectRoot, "target", "classes"))
	).flatten().filterNotNull().toSet()
}

/** Joins the segments to a path and returns it if it exists or null otherwise. */
private fun resolveIfExists(root: Path, vararg segments: String): Path? {
    var result = root
    for (segment in segments) {
        result = result.resolve(segment)
    }
    return if (Files.exists(result)) result else null
}
