package com.fwcd.ktda.util

import java.io.OutputStream
import com.fwcd.ktda.LOG

class LoggingOutputStream(
	private val downstream: OutputStream,
	private val logEnabled: Boolean,
	private val bufferLines: Boolean
): OutputStream() {
	private val newline = System.lineSeparator()
	private val buffer = StringBuilder()
	private val printStream = DelegatePrintStream {
		if (bufferLines) {
			buffer.append(it)
			if (it.contains(newline)) {
				LOG.info("OUT << $buffer")
				buffer.setLength(0)
			}
		} else LOG.info("OUT << $it")
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
