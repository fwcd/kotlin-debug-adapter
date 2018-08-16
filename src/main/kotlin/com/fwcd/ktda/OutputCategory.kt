package com.fwcd.ktda

/**
 * Type-safe wrapper around an output category as specified on
 * https://microsoft.github.io/debug-adapter-protocol/specification#Events_Output
 */
enum class OutputCategory(val value: String) {
	CONSOLE("console"),
	STDOUT("stdout"),
	STDERR("stderr"),
	TELEMETRY("telemetry")
}
