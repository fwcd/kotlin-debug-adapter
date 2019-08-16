package org.javacs.ktda.util

class ObservableList<T>(
	private var entries: MutableList<T> = mutableListOf()
) {
	private val listeners = ListenerList<List<T>>()
	
	val size: Int
		get() = entries.size
	val empty: Boolean
		get() = entries.isEmpty()
	
	fun add(element: T) {
		entries.add(element)
		fire()
	}
	
	fun remove(element: T) {
		entries.remove(element)
		fire()
	}
	
	fun get(): List<T> = entries
	
	operator fun get(index: Int) = entries[index]
	
	operator fun set(index: Int, value: T) {
		entries[index] = value
		fire()
	}
	
	fun setAll(values: List<T>) {
		entries = values.toMutableList()
		fire()
	}
	
	fun asSequence(): Sequence<T> = entries.asSequence()
	
	fun listen(listener: (List<T>) -> Unit) = listeners.add(listener)
	
	fun listenAndFire(listener: (List<T>) -> Unit) = listeners.add(listener).also { listener(entries) }
	
	fun unlisten(listener: (List<T>) -> Unit) = listeners.remove(listener)
	
	private fun fire() = listeners.fire(entries)
}
