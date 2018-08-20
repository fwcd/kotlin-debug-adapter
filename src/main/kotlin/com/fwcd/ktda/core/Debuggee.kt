package com.fwcd.ktda.core

interface Debuggee {
	val threads: List<DebuggeeThread>
	
	fun start()
	
	fun stop()
	
	fun stepOver()
	
	fun stepInto()
	
	fun stepOut()
}
