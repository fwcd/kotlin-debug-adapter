package com.fwcd.ktda.classpath

import com.fwcd.ktda.LOG

private val fileSeparator by lazy { "[/\\\\]".toRegex() }

/**
 * Converts a file path to multiple possible JVM class names.
 * 
 * For example:
 *
 * ".../src/main/kotlin/com/abc/MyClass.kt" will be converted to
 * [com.abc.MyClass, com.abc.MyClassKt]
 */
fun toJVMClassNames(filePath: String): List<String> {
	// TODO: Implement this using the Kotlin compiler API instead
	// See https://github.com/JetBrains/kotlin-netbeans/blob/c3360e8c89c1d4dac1e6f18267052ff740705079/src/main/java/org/jetbrains/kotlin/debugger/KotlinDebugUtils.java#L166-L194
	
	val rawClassName = filePath.split(fileSeparator)
		.takeLastWhile { it != "kotlin" } // Assuming .../src/main/kotlin/... directory structure
		.joinToString(separator = ".")
	val className = if (filePath.endsWith(".kt")) {
			rawClassName.dropLast(3)
		} else if (filePath.endsWith(".kts")) {
			rawClassName.dropLast(4)
		} else throw IllegalArgumentException("Can't convert non-Kotlin source file '" + filePath + "' to JVM class name")
	LOG.info(rawClassName) // DEBUG
	val ktClassName = className
		.capitalizeCharAt(className.lastIndexOf(".") + 1) + "Kt" // Class name to PascalCase
	
	return listOf(className, ktClassName)
}

private fun String.capitalizeCharAt(index: Int) =
	take(index) + this[index].toUpperCase() + substring(index + 1)
