package com.fwcd.ktda.util

class ListenerList<T> {
	private val listeners = mutableListOf<(T) -> Unit>()
	
	fun fire(event: T) = listeners.forEach { it(event) }
	
	fun add(listener: (T) -> Unit) = listeners.add(listener)
	
	fun remove(listener: (T) -> Unit) = listeners.remove(listener)
	
	fun propagateTo(next: ListenerList<T>) = add(next::fire)
}
