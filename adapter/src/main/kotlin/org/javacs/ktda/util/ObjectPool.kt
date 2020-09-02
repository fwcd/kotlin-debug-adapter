package org.javacs.ktda.util

import org.javacs.ktda.util.Identifiable

private data class ObjectKey<O>(
	val id: Long,
	val owner: O
)

private data class ObjectMapping<O, V> (
	val key: ObjectKey<O>,
	val value: V
)

/**
 * Maps objects of owners to multiple owned values.
 * To store and retrieve objects, unique ids are used.
 */
class ObjectPool<O, V> {
	private val mappingsByID = mutableMapOf<Long, ObjectMapping<O, V>>()
	private val mappingsByOwner = mutableMapOf<O, MutableSet<ObjectMapping<O, V>>>()

	private var currentID = 1L
	
	val empty: Boolean
		get() = mappingsByID.isEmpty()
	val size: Int
		get() = mappingsByID.size
	
	fun clear() {
		mappingsByID.clear()
		mappingsByOwner.clear()
	}
	
	/** Stores an object and returns its (unique) id */
	fun store(owner: O, value: V): Long {
		val id = (value as? Identifiable)?.id ?: nextID()
		val key = ObjectKey(id, owner)
		val mapping = ObjectMapping(key, value)
		
		mappingsByID[id] = mapping
		mappingsByOwner.putIfAbsent(owner, mutableSetOf())
		mappingsByOwner[owner]!!.add(mapping)
		
		return id
	}
	
	fun removeAllOwnedBy(owner: O) {
		mappingsByOwner[owner]?.let {
			it.forEach { mapping ->
				mappingsByID.remove(mapping.key.id)
			}
		}
		mappingsByOwner.remove(owner)
	}
	
	fun removeByID(id: Long) {
		mappingsByID[id]?.let {
			mappingsByOwner[it.key.owner]?.remove(it)
		}
		mappingsByID.remove(id)
	}
	
	fun getByID(id: Long) = mappingsByID[id]?.value

	fun getIDsOwnedBy(owner: O): Set<Long> = mappingsByOwner[owner]
		?.map { it.key.id }
		?.toSet()
		.orEmpty()
	
	fun getOwnedBy(owner: O): Set<V> = mappingsByOwner[owner]
		?.map { it.value }
		?.toSet()
		.orEmpty()
	
	fun containsID(id: Long) = mappingsByID.contains(id)

	private fun nextID(): Long {
		do {
			currentID++
		} while (containsID(currentID));

		return currentID
	}
}
