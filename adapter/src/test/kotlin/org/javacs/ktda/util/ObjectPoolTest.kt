package org.javacs.ktda.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ObjectPoolTest {
	@Test
	fun testObjectPool() {
		val pool = ObjectPool<String, String>()
		assertEquals(0, pool.size)
		
		val parkID = pool.store("city", "park")
		val riverID = pool.store("city", "river")
		val streetsID = pool.store("city", "streets")
		
		val doorID = pool.store("house", "door")
		val kitchenID = pool.store("house", "kitchen")
		val livingRoomID = pool.store("house", "livingRoom")
		
		assertEquals(6, pool.size)
		assertEquals(setOf("park", "river", "streets"), pool.getOwnedBy("city"))
		assertEquals(setOf("door", "kitchen", "livingRoom"), pool.getOwnedBy("house"))
		assertEquals("park", pool.getByID(parkID))
		assertEquals("river", pool.getByID(riverID))
		assertEquals("streets", pool.getByID(streetsID))
		assertEquals("door", pool.getByID(doorID))
		assertEquals("kitchen", pool.getByID(kitchenID))
		assertEquals("livingRoom", pool.getByID(livingRoomID))
		
		pool.removeAllOwnedBy("city")
		assertEquals(3, pool.size)
		
		pool.removeByID(doorID)
		assertEquals(2, pool.size)
		assertEquals(setOf("kitchen", "livingRoom"), pool.getOwnedBy("house"))
		
		pool.removeAllOwnedBy("house")
		assertEquals(0, pool.size)
	}
}
