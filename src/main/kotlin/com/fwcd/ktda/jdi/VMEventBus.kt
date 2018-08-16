package com.fwcd.ktda.jdi

import com.fwcd.ktda.LOG
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VMDisconnectedException

/**
 * Asynchronously polls and publishes any events from
 * a debuggee virtual machine. 
 */
class VMEventBus(private val vm: VirtualMachine) {
	private val listeners = mutableListOf<(DebugEvent) -> Unit>()
	private var stopped = false
	
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
						fire(event)
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
		}, "VM EventBus").start()
	}
	
	private fun fire(event: DebugEvent) = listeners.forEach { it(event) }
	
	fun subscribe(listener: (DebugEvent) -> Unit) = listeners.add(listener)
	
	fun unsubscribe(listener: (DebugEvent) -> Unit) = listeners.remove(listener)
}
