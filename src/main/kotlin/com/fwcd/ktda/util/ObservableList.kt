package com.fwcd.ktda.util

class ObservableList<T>(
	private val entries: MutableList<T> = mutableListOf()
) {
	private val listeners = ListenerList<List<T>>()
	
	val size: Int
		get() = entries.size
	
	fun add(element: T) {
		entries.add(element)
		fire()
	}
	
	fun remove(element: T) {
		entries.remove(element)
		fire()
	}
	
	operator fun get(index: Int) = entries[index]
	
	operator fun set(index: Int, value: T) {
		entries[index] = value
		fire()
	}
	
	fun asSequence(): Sequence<T> = entries.asSequence()
	
	fun listen(listener: (List<T>) -> Unit) = listeners.add(listener)
	
	fun unlisten(listener: (List<T>) -> Unit) = listeners.remove(listener)
	
	private fun fire() = listeners.fire(entries)
}
