package org.javacs.ktda.jdi.event

import org.javacs.kt.LOG
import org.javacs.ktda.util.Box
import org.javacs.ktda.util.ListenerList
import org.javacs.ktda.util.Subscription
import org.javacs.ktda.core.event.DebuggeeEventBus
import org.javacs.ktda.core.exception.DebuggeeException
import org.javacs.ktda.core.event.ExitEvent
import org.javacs.ktda.core.event.BreakpointStopEvent
import org.javacs.ktda.core.event.ExceptionStopEvent
import org.javacs.ktda.core.event.StepStopEvent
import org.javacs.ktda.core.event.ThreadEvent
import org.javacs.ktda.core.event.ThreadEventReason
import org.javacs.ktda.jdi.exception.JDIException
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.event.VMDeathEvent
import com.sun.jdi.event.Event as JDIEvent
import com.sun.jdi.event.LocatableEvent as JDILocatableEvent
import com.sun.jdi.event.EventSet as JDIEventSet
import com.sun.jdi.event.BreakpointEvent as JDIBreakpointEvent
import com.sun.jdi.event.StepEvent as JDIStepEvent
import com.sun.jdi.event.ExceptionEvent as JDIExceptionEvent
import com.sun.jdi.event.ThreadStartEvent as JDIThreadStartEvent
import com.sun.jdi.event.ThreadDeathEvent as JDIThreadDeathEvent
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Asynchronously polls and publishes any events from
 * a debuggee virtual machine. 
 */
class VMEventBus(private val vm: VirtualMachine): DebuggeeEventBus {
	private var exited = false
	private val eventListeners = ConcurrentHashMap<KClass<out JDIEvent>, ListenerList<VMEvent<JDIEvent>>>()
	override val exitListeners = ListenerList<ExitEvent>()
	override val breakpointListeners = ListenerList<BreakpointStopEvent>()
	override val stepListeners = ListenerList<StepStopEvent>()
	override val exceptionListeners = ListenerList<ExceptionStopEvent>()
	override val threadListeners = ListenerList<ThreadEvent>()
	
	init {
		hookListeners()
		startAsyncPoller()
	}
	
	private fun startAsyncPoller() {
		Thread({
			val eventQueue = vm.eventQueue()
			try {
				while (!exited) {
					val eventSet = eventQueue.remove()
					var resumeThreads = true
					
					for (event in eventSet) {
						LOG.debug("VM Event: {}", event)
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
			exitListeners.fire(ExitEvent)
		}, "eventBus").start()
	}
	
	private fun hookListeners() {
		val eventRequestManager = vm.eventRequestManager()
		eventRequestManager.createThreadStartRequest().enable()
		eventRequestManager.createThreadDeathRequest().enable()
		eventRequestManager.createVMDeathRequest().enable()

		subscribe(JDIBreakpointEvent::class) {
			breakpointListeners.fire(BreakpointStopEvent(
				threadID = toThreadID(it.jdiEvent)
			))
			it.resumeThreads = false
		}
		subscribe(JDIStepEvent::class) {
			stepListeners.fire(StepStopEvent(
				threadID = toThreadID(it.jdiEvent)
			))
			it.resumeThreads = false
		}
		subscribe(JDIExceptionEvent::class) {
			exceptionListeners.fire(ExceptionStopEvent(
				threadID = toThreadID(it.jdiEvent),
				exception = JDIException(it.jdiEvent.exception(), it.jdiEvent.thread())
			))
			it.resumeThreads = false
		}
		subscribe(JDIThreadStartEvent::class) {
			threadListeners.fire(ThreadEvent(
				threadID = it.jdiEvent.thread().uniqueID(),
				reason = ThreadEventReason.STARTED
			))
		}
		subscribe(JDIThreadDeathEvent::class) {
			threadListeners.fire(ThreadEvent(
				threadID = it.jdiEvent.thread().uniqueID(),
				reason = ThreadEventReason.STOPPED
			))
		}
	}
	
	private fun toThreadID(event: JDILocatableEvent) = event.thread().uniqueID()
	
	/** Subscribes to a JDI event type and lets the caller decide when to stop subscribing. */
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
