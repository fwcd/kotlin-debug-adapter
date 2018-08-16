package com.fwcd.ktda.util

import com.fwcd.ktda.LOG
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

fun <T> tryResolving(what: String, resolver: () -> T?): T? {
    try {
        val resolved = resolver()
        if (resolved != null) {
            LOG.info("Successfully resolved $what")
            return resolved
        } else {
            LOG.info("Could not resolve $what as it is null")
        }
    } catch (e: Exception) {
        LOG.info("Could not resolve $what: ${e.message}")
    }
    return null
}
