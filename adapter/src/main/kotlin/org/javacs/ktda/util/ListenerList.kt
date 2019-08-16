package org.javacs.ktda.util

import java.util.ArrayDeque

typealias Listener<T> = (T) -> Unit

class ListenerList<T> {
	private val listeners = mutableListOf<Listener<T>>()
	private val queuedModifications = ArrayDeque<(MutableList<Listener<T>>) -> Unit>()
	@Volatile private var iterators = 0
	
	fun fire(event: T) {
		iterators += 1
		listeners.forEach { it(event) }
		iterators -= 1
		
		if (iterators <= 0) {
			applyModifications()
		}
	}
	
	fun add(listener: Listener<T>) = withListeners { it.add(listener) }
	
	fun remove(listener: Listener<T>) = withListeners { it.remove(listener) }
	
	fun propagateTo(next: ListenerList<T>) = add(next::fire)
	
	private fun applyModifications() {
		while (!queuedModifications.isEmpty()) {
			queuedModifications.poll()(listeners)
		}
	}
	
	private fun withListeners(body: (MutableList<Listener<T>>) -> Unit) {
		if (iterators > 0) {
			// Do not modify listener list concurrently
			queuedModifications.offer(body)
		} else body(listeners)
	}
}
