package org.javacs.ktda.util

import java.io.OutputStream
import org.javacs.kt.util.DelegatePrintStream

private val MESSAGE_FLUSH_MIN_LENGTH = 20

class LoggingOutputStream(
	private val downstream: OutputStream,
	private val logEnabled: Boolean,
	private val bufferLines: Boolean
) : OutputStream() {
	private val newline = System.lineSeparator()
	private val buffer = StringBuilder()
	private val printStream = DelegatePrintStream {
		if (bufferLines) {
			buffer.append(it)
			if (it.contains(newline)) {
				JSON_LOG.info("OUT << {}", buffer)
				buffer.setLength(0)
			}
		} else JSON_LOG.info("OUT << {}", it)
	}
	
	override fun write(b: Int) {
		if (logEnabled) {
			printStream.write(b)
		}
		downstream.write(b)
	}
	
	override fun write(b: ByteArray) {
		if (logEnabled) {
			printStream.write(b)
		}
		downstream.write(b)
	}
	
	override fun write(b: ByteArray, off: Int, len: Int) {
		if (logEnabled) {
			printStream.write(b, off, len)
		}
		downstream.write(b, off, len)
	}
}
