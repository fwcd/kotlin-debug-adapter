package com.fwcd.ktda.core

import com.fwcd.ktda.core.stack.StackTrace

interface DebuggeeThread {
	fun pause()
	
	fun resume()
	
	fun stackTrace(): StackTrace
}
