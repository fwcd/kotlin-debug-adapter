package org.javacs.ktda.util

class ObservableSet<T>(
	private var entries: MutableSet<T> = mutableSetOf()
) {
	private val listeners = ListenerList<Set<T>>()
	
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
	
	fun get(): Set<T> = entries
	
	fun setAll(values: Set<T>) {
		entries = values.toMutableSet()
		fire()
	}
	
	fun asSequence(): Sequence<T> = entries.asSequence()
	
	fun listen(listener: (Set<T>) -> Unit) = listeners.add(listener)
	
	fun listenAndFire(listener: (Set<T>) -> Unit) = listeners.add(listener).also { listener(entries) }
	
	fun unlisten(listener: (Set<T>) -> Unit) = listeners.remove(listener)
	
	private fun fire() = listeners.fire(entries)
}
