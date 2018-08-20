package com.fwcd.ktda.jdi.event

import com.fwcd.ktda.LOG
import com.fwcd.ktda.util.ListenerList
import com.fwcd.ktda.util.Subscription
import com.fwcd.ktda.core.event.DebuggeeEventBus
import com.fwcd.ktda.core.event.StopEvent
import com.fwcd.ktda.core.event.BreakpointPauseEvent
import com.fwcd.ktda.core.event.ExceptionPauseEvent
import com.fwcd.ktda.core.event.StepPauseEvent
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.event.Event
import com.sun.jdi.event.LocatableEvent
import com.sun.jdi.event.EventSet
import kotlin.reflect.KClass

/**
 * Asynchronously polls and publishes any events from
 * a debuggee virtual machine. 
 */
class VMEventBus(private val vm: VirtualMachine): DebuggeeEventBus {
	private var stopped = false
	private val eventListeners = mutableMapOf<KClass<out Event>, ListenerList<VMEvent<Event>>>()
	override val stopListeners = ListenerList<StopEvent>()
	override val breakpointListeners = ListenerList<BreakpointPauseEvent>()
	override val stepListeners = ListenerList<StepPauseEvent>()
	override var exceptionListeners = ListenerList<ExceptionPauseEvent>()
	
	init {
		startAsyncPoller()
		hookListeners()
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
			stopListeners.fire(StopEvent())
		}, "VMEventBus").start()
	}
	
	private fun hookListeners() {
		subscribe(com.sun.jdi.event.BreakpointEvent::class) {
			breakpointListeners.fire(BreakpointPauseEvent(
				threadID = toThreadID(it.jdiEvent)
			))
			it.resumeThreads = false
		}
		subscribe(com.sun.jdi.event.StepEvent::class) {
			stepListeners.fire(StepPauseEvent(
				threadID = toThreadID(it.jdiEvent)
			))
			it.resumeThreads = false
		}
		subscribe(com.sun.jdi.event.ExceptionEvent::class) {
			val exception = it.jdiEvent.exception()
			exceptionListeners.fire(ExceptionPauseEvent(
				threadID = toThreadID(it.jdiEvent),
				exceptionName = exception.referenceType().name()
			))
		}
	}
	
	private fun toThreadID(event: LocatableEvent) = event.thread().uniqueID()
	
	@Suppress("UNCHECKED_CAST")
	fun <E: Event> subscribe(eventClass: KClass<E>, listener: (VMEvent<E>) -> Unit): Subscription {
		eventListeners.putIfAbsent(eventClass, ListenerList())
		// This cast is safe, because dispatchEvent uses
		// reflection to assure that only a correct 'Event' type is passed
		// and due to type erasure on JVM
		eventListeners[eventClass]!!.add(listener as (VMEvent<Event>) -> Unit)
		return object: Subscription {
			override fun unsubscribe() {
				eventListeners[eventClass]?.remove(listener as (VMEvent<Event>) -> Unit)
			}
		}
	}
	
	private fun dispatchEvent(event: Event, eventSet: EventSet): Boolean {
		val VMEvent = VMEvent(event, eventSet)
		val eventClass = event::class.java
		eventListeners
			.filterKeys { it.java.isAssignableFrom(eventClass) }
			.values
			.forEach { it.fire(VMEvent) }
		return VMEvent.resumeThreads
	}
}
