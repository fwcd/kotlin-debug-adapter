package com.fwcd.ktda.util

import java.io.InputStream
import com.fwcd.ktda.LOG

class LoggingInputStream(
	private val upstream: InputStream,
	private val logEnabled: Boolean,
	private val bufferLines: Boolean
): InputStream() {
	private val newline = System.lineSeparator()
	private val buffer = StringBuilder()
	private val printStream = DelegatePrintStream {
		if (bufferLines) {
			buffer.append(it)
			if (it.contains(newline)) {
				LOG.info("IN >> $buffer")
				buffer.setLength(0)
			}
		} else LOG.info("IN >> $it")
	}
	
	override fun read(): Int {
		val result = upstream.read()
		if (logEnabled) {
			printStream.write(result)
		}
		return result
	}
	
	override fun read(b: ByteArray): Int {
		val result = upstream.read(b)
		if (logEnabled) {
			printStream.write(b)
		}
		return result
	}
	
	override fun read(b: ByteArray, off: Int, len: Int): Int {
		val result = upstream.read(b, off, len)
		if (logEnabled) {
			printStream.write(b, off, len)
		}
		return result
	}
}
