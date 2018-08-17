package com.fwcd.ktda.jdi

import com.fwcd.ktda.LOG
import com.fwcd.ktda.util.ListenerList
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VMDisconnectedException

/**
 * Asynchronously polls and publishes any events from
 * a debuggee virtual machine. 
 */
class VMEventPoller(private val vm: VirtualMachine) {
	private var stopped = false
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
	
	private fun dispatchEvent(event: DebugEvent) {
		// TODO
	}
}
