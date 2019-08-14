package fwcd.ktda.jdi.event

import fwcd.ktda.LOG
import fwcd.ktda.util.ListenerList
import fwcd.ktda.util.Subscription
import fwcd.ktda.core.event.DebuggeeEventBus
import fwcd.ktda.core.event.StopEvent
import fwcd.ktda.core.event.BreakpointPauseEvent
import fwcd.ktda.core.event.ExceptionPauseEvent
import fwcd.ktda.core.event.StepPauseEvent
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.event.VMDeathEvent
import com.sun.jdi.event.Event as JDIEvent
import com.sun.jdi.event.LocatableEvent as JDILocatableEvent
import com.sun.jdi.event.EventSet as JDIEventSet
import com.sun.jdi.event.StepEvent as JDIStepEvent
import com.sun.jdi.event.ExceptionEvent as JDIExceptionEvent
import kotlin.reflect.KClass

/**
 * Asynchronously polls and publishes any events from
 * a debuggee virtual machine. 
 */
class VMEventBus(private val vm: VirtualMachine): DebuggeeEventBus {
	private var exited = false
	private val eventListeners = mutableMapOf<KClass<out JDIEvent>, ListenerList<VMEvent<JDIEvent>>>()
	override val exitListeners = ListenerList<StopEvent>()
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
				while (!exited) {
					val eventSet = eventQueue.remove()
					var resumeThreads = true
					
					for (event in eventSet) {
						// LOG.info("VM Event: {}", event::class.simpleName) // DEBUG
						if (event is VMDeathEvent) {
							exited = true
							resumeThreads = false
						} else {
							val resume = dispatchEvent(event, eventSet)
							resumeThreads = resumeThreads && resume
						}
					}
					
					if (resumeThreads) {
						eventSet.resume()
					}
				}
			} catch (e: InterruptedException) {
				LOG.debug("VMEventBus event poller terminated by interrupt")
			} catch (e: VMDisconnectedException) {
				LOG.info("VMEventBus event poller terminated by disconnect: {}", e.message)
			}
			exitListeners.fire(StopEvent())
		}, "VMEventBus").start()
	}
	
	private fun hookListeners() {
		subscribe(com.sun.jdi.event.BreakpointEvent::class) {
			breakpointListeners.fire(BreakpointPauseEvent(
				threadID = toThreadID(it.jdiEvent)
			))
			it.resumeThreads = false
		}
		subscribe(JDIStepEvent::class) {
			stepListeners.fire(StepPauseEvent(
				threadID = toThreadID(it.jdiEvent)
			))
			it.resumeThreads = false
		}
		subscribe(JDIExceptionEvent::class) {
			val exception = it.jdiEvent.exception()
			exceptionListeners.fire(ExceptionPauseEvent(
				threadID = toThreadID(it.jdiEvent),
				exceptionName = exception.referenceType().name()
			))
		}
	}
	
	private fun toThreadID(event: JDILocatableEvent) = event.thread().uniqueID()
	
	@Suppress("UNCHECKED_CAST")
	fun <E: JDIEvent> subscribe(eventClass: KClass<E>, listener: (VMEvent<E>) -> Unit): Subscription {
		eventListeners.putIfAbsent(eventClass, ListenerList())
		// This cast is safe, because dispatchEvent uses
		// reflection to assure that only a correct 'Event' type is passed
		// and due to type erasure on JVM
		eventListeners[eventClass]!!.add(listener as (VMEvent<JDIEvent>) -> Unit)
		return object: Subscription {
			override fun unsubscribe() {
				eventListeners[eventClass]?.remove(listener as (VMEvent<JDIEvent>) -> Unit)
			}
		}
	}
	
	private fun dispatchEvent(event: JDIEvent, eventSet: JDIEventSet): Boolean {
		val VMEvent = VMEvent(event, eventSet)
		val eventClass = event::class.java
		eventListeners
			.filterKeys { it.java.isAssignableFrom(eventClass) }
			.values
			.forEach { it.fire(VMEvent) }
		return VMEvent.resumeThreads
	}
}
