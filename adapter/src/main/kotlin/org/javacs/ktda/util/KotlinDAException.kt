package org.javacs.ktda.util

/**
 * An exception related to the debug adapter
 */
class KotlinDAException : RuntimeException {
	constructor(msg: String) : super(msg)
	
	constructor(msg: String, cause: Throwable) : super(msg, cause)
}
