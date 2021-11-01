package org.javacs.ktda.classpath

import org.javacs.kt.LOG
import org.javacs.ktda.util.firstNonNull
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.Path

private val fileSeparator by lazy { "[/\\\\]".toRegex() }
private val sourceFileExtensions = setOf(".kt", ".kts", ".java")

/**
 * Converts a file path to multiple possible JVM class names.
 * 
 * For example:
 *
 * ".../src/main/kotlin/com/abc/MyClass.kt" will be converted to
 * [com.abc.MyClass, com.abc.MyClassKt]
 */
fun toJVMClassNames(filePath: String): List<String>? {
	// TODO: Implement this using the Kotlin compiler API instead
	// See https://github.com/JetBrains/kotlin-netbeans/blob/c3360e8c89c1d4dac1e6f18267052ff740705079/src/main/java/org/jetbrains/kotlin/debugger/KotlinDebugUtils.java#L166-L194
	
	val rawClassName = filePath.split(fileSeparator) // TODO: Use Project.sourcesRoot instead
		.takeLastWhile { it != "kotlin" && it != "java" } // Assuming .../src/main/kotlin/... directory structure
		.joinToString(separator = ".")
	val className = sourceFileExtensions
		.asSequence()
		.find { filePath.endsWith(it) }
		?.let { rawClassName.dropLast(it.length) }
		?: return null
	val ktClassName = className
		.capitalizeCharAt(className.lastIndexOf(".") + 1) + "Kt" // Class name to PascalCase
	
	return listOf(className, ktClassName)
}

// TODO: Better path resolution, especially when dealing with
// *.class files inside JARs
fun findValidKtFilePath(filePathToClass: Path, sourceName: String?) =
	filePathToClass.resolveSibling(sourceName).ifExists()
	?: filePathToClass.withExtension(".kt").ifExists()

private fun Path.ifExists() = if (Files.exists(this)) this else null

private fun Path.withExtension(extension: String) = resolveSibling(fileName.toString() + extension)

private fun String.capitalizeCharAt(index: Int) =
	take(index) + this[index].uppercaseChar() + substring(index + 1)
