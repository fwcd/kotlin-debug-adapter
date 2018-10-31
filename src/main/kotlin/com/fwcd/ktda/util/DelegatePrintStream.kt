package com.fwcd.ktda.util

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.function.Consumer

class DelegatePrintStream(private val delegate: (String) -> Unit): PrintStream(ByteArrayOutputStream(0)) {
	private val newLine = System.lineSeparator()
	
	override fun write(c: Int) = delegate((c.toChar()).toString())
	
	override fun write(buf: ByteArray, off: Int, len: Int) {
		if (len > 0 && buf.size > 0) {
			delegate(String(buf, off, len))
		}
	}
	
	override fun append(csq: CharSequence): PrintStream {
		delegate(csq.toString())
		return this
	}
	
	override fun append(csq: CharSequence, start: Int, end: Int): PrintStream {
		delegate(csq.subSequence(start, end).toString())
		return this
	}
	
	override fun append(c:Char): PrintStream {
		delegate((c).toString())
		return this
	}
	
	override fun print(b: Boolean) = delegate(java.lang.Boolean.toString(b))
	
	override fun print(c: Char) = delegate(Character.toString(c))
	
	override fun print(i: Int) = delegate(Integer.toString(i))
	
	override fun print(l: Long) = delegate(java.lang.Long.toString(l))
	
	override fun print(f: Float) = delegate(java.lang.Float.toString(f))
	
	override fun print(d: Double) = delegate(java.lang.Double.toString(d))
	
	override fun print(s: CharArray) = delegate(String(s))
	
	override fun print(s: String) = delegate(s)
	
	override fun print(obj: Any) = delegate(obj.toString())
	
	override fun println() = delegate(newLine)
	
	override fun println(x: Boolean) = delegate(java.lang.Boolean.toString(x) + newLine)
	
	override fun println(x: Char) = delegate(Character.toString(x) + newLine)
	
	override fun println(x: Int) = delegate(Integer.toString(x) + newLine)
	
	override fun println(x: Long) = delegate(java.lang.Long.toString(x) + newLine)
	
	override fun println(x: Float) = delegate(java.lang.Float.toString(x) + newLine)
	
	override fun println(x: Double) = delegate(java.lang.Double.toString(x) + newLine)
	
	override fun println(x: CharArray) = delegate(String(x) + newLine)
	
	override fun println(x: String) = delegate(x + newLine)
	
	override fun println(x: Any) = delegate(x.toString() + newLine)
}
