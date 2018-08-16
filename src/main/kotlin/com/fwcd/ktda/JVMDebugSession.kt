package com.fwcd.ktda

import com.fwcd.ktda.LOG
import java.nio.file.Path

/**
 * The debugging backend that uses the Java Debug Interface.
 */
class JVMDebugSession(
	private val classpath: Set<Path>,
	private val mainClass: String
) {
	fun start() {
		LOG.info("Starting JVM debug session with classpath $classpath and mainClass $mainClass")
	}
	
	fun stop() {
		LOG.info("Stopping JVM debug session")
	}
}
