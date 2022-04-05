package org.javacs.ktda.classpath

import java.nio.file.Path
import java.nio.file.Files
import org.javacs.kt.classpath.ClassPathEntry
import org.javacs.kt.classpath.ClassPathResolver

/** Resolver for the project's own (compiled) class files. */
internal class ProjectClassesResolver(private val projectRoot: Path) : ClassPathResolver {
	override val resolverType: String = "Project classes"
	override val classpath: Set<ClassPathEntry> get() = sequenceOf(
		// Gradle
		sequenceOf("kotlin", "java").flatMap { language ->
			sequenceOf("main", "test").flatMap { sourceSet ->
				sequenceOf(
					resolveIfExists(projectRoot, "build", "classes", language, sourceSet),
					// kotlin multiplatform project jvm build path
					resolveIfExists(projectRoot, "build", "classes", language, "jvm", sourceSet)
				)
			}
		},
		// Maven
		sequenceOf(resolveIfExists(projectRoot, "target", "classes")),
		// Spring Boot application.properties and templates.
		sequenceOf(resolveIfExists(projectRoot, "build", "resources", "main"))
	).flatten().filterNotNull().map(::ClassPathEntry).toSet()
}

/** Joins the segments to a path and returns it if it exists or null otherwise. */
private fun resolveIfExists(root: Path, vararg segments: String): Path? {
    var result = root
    for (segment in segments) {
        result = result.resolve(segment)
    }
    return result.takeIf { Files.exists(it) }
}
