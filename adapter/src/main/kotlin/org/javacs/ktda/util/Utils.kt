package org.javacs.ktda.util

import org.javacs.kt.LOG
import java.nio.file.Path
import java.nio.file.Paths

fun execAndReadStdout(shellCommand: String, directory: Path): String {
    val process = Runtime.getRuntime().exec(shellCommand, null, directory.toFile())
    val stdout = process.inputStream
    var result = ""
    stdout.bufferedReader().use {
        result = it.readText()
    }
    return result
}

fun winCompatiblePathOf(path: String): Path {
    if (path.get(2) == ':' && path.get(0) == '/') {
        // Strip leading '/' when dealing with paths on Windows
        return Paths.get(path.substring(1))
    } else {
        return Paths.get(path)
    }
}

fun Path.replaceExtensionWith(newExtension: String): Path {
	val oldName = fileName.toString()
	val newName = oldName.substring(0, oldName.lastIndexOf(".")) + newExtension
	return resolveSibling(newName)
}

inline fun <T, C: Collection<T>> C.ifEmpty(then: () -> C) = if (isEmpty()) then() else this

fun <T> firstNonNull(vararg optionals: () -> T?): T? {
    for (optional in optionals) {
        val result = optional()
        if (result != null) {
            return result
        }
    }
    return null
}

fun <T> nonNull(item: T?, errorMsgIfNull: String): T =
    if (item == null) {
        throw NullPointerException(errorMsgIfNull)
    } else item

/**
 * Blocks the current thread until the condition becomes true.
 * Checks are performed in 80 ms intervals.
 */
inline fun waitFor(what: String, condition: () -> Boolean) {
    val delayUntilNotificationMs = 10_000
    val startTime = System.currentTimeMillis()
    var lastTime = startTime
    
    while (!condition()) {
        Thread.sleep(80)
        
        val now = System.currentTimeMillis()
        if ((now - lastTime) > delayUntilNotificationMs) {
            LOG.info("Waiting for {} for {} seconds...", what, (now - startTime) / 1000)
            lastTime = now
        }
    }
}

fun <T> tryResolving(what: String, resolver: () -> T?): T? {
    try {
        val resolved = resolver()
        if (resolved != null) {
            LOG.debug("Successfully resolved {}", what)
            return resolved
        } else {
            LOG.debug("Could not resolve {} as it is null", what)
        }
    } catch (e: Exception) {
        LOG.debug("Could not resolve {}: {}", what, e.message)
    }
    return null
}
