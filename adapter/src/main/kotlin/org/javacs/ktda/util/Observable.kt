package org.javacs.ktda.util

class Observable<T>(private var value: T) {
	private val listeners = ListenerList<T>()
	
	fun set(value: T) {
		this.value = value
		fire()
	}
	
	fun listen(listener: (T) -> Unit) = listeners.add(listener)
	
	fun unlisten(listener: (T) -> Unit) = listeners.remove(listener)
	
	fun get() = value
	
	fun fire() = listeners.fire(value)
}
