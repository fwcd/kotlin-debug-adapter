package org.javacs.ktda.util

class ObservableMap<K, V>(
	private var entries: MutableMap<K, V> = mutableMapOf()
) {
	private val listeners = ListenerList<Map<K, V>>()
	
	val size: Int
		get() = entries.size
	val empty: Boolean
		get() = entries.isEmpty()
	
	fun remove(key: K) = entries.remove(key).also { fire() }
	
	operator fun set(key: K, value: V) {
		entries[key] = value
		fire()
	}
	
	operator fun get(key: K) = entries[key]
	
	fun get(): Map<K, V> = entries
	
	fun listen(listener: (Map<K, V>) -> Unit) = listeners.add(listener)
	
	fun listenAndFire(listener: (Map<K, V>) -> Unit) = listeners.add(listener).also { listener(entries) }
	
	fun unlisten(listener: (Map<K, V>) -> Unit) = listeners.remove(listener)
	
	private fun fire() = listeners.fire(entries)
}
