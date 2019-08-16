package org.javacs.ktda.jdi

import org.javacs.ktda.core.DebuggeeThread
import org.javacs.ktda.core.stack.StackTrace
import org.javacs.ktda.util.Subscription
import org.javacs.ktda.jdi.stack.JDIStackTrace
import org.javacs.ktda.jdi.JDISessionContext
import com.sun.jdi.ThreadReference
import com.sun.jdi.event.Event
import com.sun.jdi.request.EventRequest
import com.sun.jdi.request.StepRequest
import kotlin.reflect.KClass

class JDIThread(
	private val threadRef: ThreadReference,
	private val context: JDISessionContext
) : DebuggeeThread {
	override val name: String = threadRef.name() ?: "Unnamed Thread"
	override val id: Long = threadRef.uniqueID()
	
	override fun pause() =
		if (!threadRef.isSuspended()) {
			threadRef.suspend()
			true
		} else false
	
	override fun resume(): Boolean {
		val suspends = threadRef.suspendCount()
		(0 until suspends).forEach {
			threadRef.resume()
		}
		return suspends > 0
	}
	
	override fun stackTrace() = JDIStackTrace(threadRef.frames(), context)
	
	override fun stepOver() = stepLine(StepRequest.STEP_OVER)
	
	override fun stepInto() = stepLine(StepRequest.STEP_INTO)
	
	override fun stepOut() = stepLine(StepRequest.STEP_OUT)
	
	private fun stepLine(depth: Int) {
		stepRequest(StepRequest.STEP_LINE, depth)
			?.let { performStep(it) }
	}
	
	private fun performStep(request: StepRequest) {
		request.enable()
		resume()
	}
	
	private fun stepRequest(size: Int, depth: Int) =
		if (context.pendingStepRequestThreadIds.contains(id)) null else {
			val eventRequestManager = threadRef.virtualMachine().eventRequestManager()
			eventRequestManager
				.createStepRequest(threadRef, size, depth)
				?.also { request ->
					request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD)
					request.addCountFilter(1)
					
					// Abort pending StepRequest when a breakpoint is hit
					context.pendingStepRequestThreadIds.add(id)
					
					fun abortUponEvent(eventClass: KClass<out Event>) {
						var sub: Subscription? = null
						
						sub = context.eventBus.subscribe(eventClass) {
							val pending = context.pendingStepRequestThreadIds.contains(id)
							if (pending) {
								eventRequestManager.deleteEventRequest(request)
								context.pendingStepRequestThreadIds.remove(id)
							}
							sub?.unsubscribe()
						}
					}
					
					abortUponEvent(com.sun.jdi.event.StepEvent::class)
					abortUponEvent(com.sun.jdi.event.BreakpointEvent::class)
				}
		}
}
