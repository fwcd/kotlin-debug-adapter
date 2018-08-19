package com.fwcd.ktda.jdi

import com.fwcd.ktda.LOG
import com.fwcd.ktda.util.ListenerList
import com.fwcd.ktda.util.Subscription
import kotlin.reflect.KClass
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.event.Event
import com.sun.jdi.event.EventSet

/**
 * Asynchronously polls and publishes any events from
 * a debuggee virtual machine. 
 */
class VMEventBus(private val vm: VirtualMachine) {
	private var stopped = false
	private val eventListeners = mutableMapOf<KClass<out Event>, ListenerList<DebugEvent<Event>>>()
	val stopListeners = ListenerList<Unit>()
	
	init {
		startAsyncPoller()
	}
	
	private fun startAsyncPoller() {
		Thread({
			val eventQueue = vm.eventQueue()
			try {
				while (!stopped) {
					val eventSet = eventQueue.remove()
					var resumeThreads = true
					for (event in eventSet) {
						// LOG.info("VM Event: ${event::class.simpleName}") // DEBUG
						val resume = dispatchEvent(event, eventSet)
						resumeThreads = resumeThreads && resume
					}
					if (resumeThreads) {
						eventSet.resume()
					}
				}
			} catch (e: InterruptedException) {
				LOG.fine("VMEventBus event poller terminated by interrupt")
			} catch (e: VMDisconnectedException) {
				LOG.info("VMEventBus event poller terminated by disconnect: ${e.message}")
			}
			stopListeners.fire(Unit)
		}, "VMEventBus").start()
	}
	
	@Suppress("UNCHECKED_CAST")
	fun <E: Event> subscribe(eventClass: KClass<E>, listener: (DebugEvent<E>) -> Unit): Subscription {
		eventListeners.putIfAbsent(eventClass, ListenerList())
		// This cast is safe, because dispatchEvent uses
		// reflection to assure that only a correct 'Event' type is passed
		// and due to type erasure on JVM
		eventListeners[eventClass]!!.add(listener as (DebugEvent<Event>) -> Unit)
		return object: Subscription {
			override fun unsubscribe() {
				eventListeners[eventClass]?.remove(listener as (DebugEvent<Event>) -> Unit)
			}
		}
	}
	
	private fun dispatchEvent(event: Event, eventSet: EventSet): Boolean {
		val debugEvent = DebugEvent(event, eventSet)
		val eventClass = event::class.java
		eventListeners
			.filterKeys { it.java.isAssignableFrom(eventClass) }
			.values
			.forEach { it.fire(debugEvent) }
		return debugEvent.resumeThreads
	}
}
