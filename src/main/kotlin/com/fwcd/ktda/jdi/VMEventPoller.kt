package com.fwcd.ktda.jdi

import com.fwcd.ktda.LOG
import com.fwcd.ktda.util.ListenerList
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.event.Event
import com.sun.jdi.event.EventSet

private class DebugEvent(
	val jdiEvent: Event,
	val jdiEventSet: EventSet
) {
	var resumeThreads = true
}

typealias JDIBreakpointEvent = com.sun.jdi.event.BreakpointEvent
typealias JDIStepEvent = com.sun.jdi.event.StepEvent

/**
 * Asynchronously polls and publishes any events from
 * a debuggee virtual machine. 
 */
class VMEventPoller(private val vm: VirtualMachine) {
	private var stopped = false
	val stopListeners = ListenerList<Unit>()
	val stepListeners = ListenerList<StepEvent>()
	val breakpointListeners = ListenerList<BreakpointEvent>()
	
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
					for (jdiEvent in eventSet) {
						val event = DebugEvent(jdiEvent, eventSet)
						dispatchEvent(event)
						resumeThreads = resumeThreads && event.resumeThreads
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
		}, "VM EventBus").start()
	}
	
	private fun dispatchEvent(debugEvent: DebugEvent) {
		val e = debugEvent.jdiEvent
		when (e) {
			is JDIBreakpointEvent -> breakpointListeners.fire(BreakpointEvent())
			is JDIStepEvent -> stepListeners.fire(StepEvent())
		}
	}
}
