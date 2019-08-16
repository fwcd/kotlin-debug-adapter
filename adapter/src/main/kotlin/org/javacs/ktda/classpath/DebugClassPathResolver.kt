package org.javacs.ktda.classpath

import org.javacs.kt.classpath.ClassPathResolver
import org.javacs.kt.classpath.defaultClassPathResolver
import org.javacs.kt.classpath.plus
import org.javacs.kt.classpath.joined
import java.nio.file.Path

fun debugClassPathResolver(workspaceRoots: Collection<Path>): ClassPathResolver =
	defaultClassPathResolver(workspaceRoots) + workspaceRoots.map { ProjectClassesResolver(it) }.joined
