package org.javacs.ktda.jdi

import org.javacs.ktda.core.Debuggee
import org.javacs.ktda.core.DebuggeeThread
import org.javacs.ktda.core.DebugContext
import org.javacs.ktda.core.Position
import org.javacs.ktda.core.Source
import org.javacs.ktda.core.launch.LaunchConfiguration
import org.javacs.ktda.core.event.DebuggeeEventBus
import org.javacs.ktda.core.breakpoint.Breakpoint
import org.javacs.ktda.core.breakpoint.ExceptionBreakpoint
import org.javacs.kt.LOG
import org.javacs.ktda.util.ObservableList
import org.javacs.ktda.util.SubscriptionBag
import org.javacs.ktda.classpath.findValidKtFilePath
import org.javacs.ktda.classpath.toJVMClassNames
import org.javacs.ktda.jdi.event.VMEventBus
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.request.EventRequest
import com.sun.jdi.AbsentInformationException
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

class JDIDebuggee(
	private val vm: VirtualMachine,
	private val sourcesRoots: Set<Path>,
	private val context: DebugContext
) : Debuggee, JDISessionContext {
	override var threads = emptyList<DebuggeeThread>()
	override val eventBus: VMEventBus
	override val pendingStepRequestThreadIds = mutableSetOf<Long>()
	override val stdin: OutputStream?
	override val stdout: InputStream?
	override val stderr: InputStream?

	private var breakpointSubscriptions = SubscriptionBag()
	
	init {
		eventBus = VMEventBus(vm)
		
		val process = vm.process()
		stdin = process?.outputStream
		stdout = process?.inputStream
		stderr = process?.errorStream
		
		LOG.trace("Updating threads")
		updateThreads()

		LOG.trace("Updating breakpoints")
		hookBreakpoints()
	}
	
	override fun updateThreads() {
		threads = vm.allThreads().map { JDIThread(it, this) }
	}
	
	private fun hookBreakpoints() {
		context.breakpointManager.also { manager ->
			manager.breakpoints.listenAndFire { setAllBreakpoints(it.values.flatten()) }
			manager.exceptionBreakpoints.listenAndFire(::setExceptionBreakpoints)
		}
	}
	
	private fun setAllBreakpoints(breakpoints: List<Breakpoint>) {
		breakpointSubscriptions.unsubscribe()
		vm.eventRequestManager().deleteAllBreakpoints()
		breakpoints.forEach { bp ->
			bp.position.let { setBreakpoint(
				it.source.filePath.toAbsolutePath().toString(),
				it.lineNumber.toLong()
			) }
		}
	}
	
	private fun setExceptionBreakpoints(breakpoints: Set<ExceptionBreakpoint>) = vm
		.eventRequestManager()
		.also { it.deleteEventRequests(it.exceptionRequests()) }
		.takeIf { breakpoints.isNotEmpty() }
		// Workaround: JDI will otherwise not enable the request correctly
		?.also { vm.allThreads() }
		?.createExceptionRequest(
			null,
			breakpoints.contains(ExceptionBreakpoint.CAUGHT),
			breakpoints.contains(ExceptionBreakpoint.UNCAUGHT)
		)
		?.apply { setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD) }
		?.enable()
		?: Unit
	
	/** Tries to set a breakpoint */
	private fun setBreakpoint(filePath: String, lineNumber: Long) {
		val eventRequestManager = vm.eventRequestManager()

		toJVMClassNames(filePath)
			?.forEach { className ->
				// Try setting breakpoint using a ClassPrepareRequest

				for (name in listOf(className, "$className$*")) { // For local types
					val request = eventRequestManager
						.createClassPrepareRequest()
						.apply { addClassFilter(className) }

					breakpointSubscriptions.add(eventBus.subscribe(ClassPrepareEvent::class) {
						if (it.jdiEvent.request() == request) {
							val referenceType = it.jdiEvent.referenceType()
							LOG.trace("Setting breakpoint at prepared type {}", referenceType.name())
							setBreakpointAtType(referenceType, lineNumber)
						}
					})
					
					request.enable()
				}
				
				// Try setting breakpoint using loaded VM classes
				
				val classPattern = "^${Regex.escape(className)}(?:\\$.*)?".toRegex()
				vm.allClasses()
					.filter { classPattern.matches(it.name()) }
					.forEach {
						LOG.trace("Setting breakpoint at known type {}", it.name())
						setBreakpointAtType(it, lineNumber)
					}
			} ?: LOG.warn("Not adding breakpoint in unrecognized source file {}", Paths.get(filePath).fileName)
	}
	
	/** Tries to set a breakpoint - Will return whether this was successful */
	private fun setBreakpointAtType(refType: ReferenceType, lineNumber: Long): Boolean {
		try {
			val location = refType
				.locationsOfLine(lineNumber.toInt())
				?.firstOrNull() ?: return false
			val request = vm.eventRequestManager()
				.createBreakpointRequest(location)
				?.apply {
					setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD)
					enable()
				}
			return request != null
		} catch (e: AbsentInformationException) {
			return false
		}
	}

	override fun resume() {
		vm.resume()
	}
	
	override fun exit() {
		LOG.info("Exiting JDI session")
		try {
			if (vm.process()?.isAlive() ?: false) {
				vm.exit(0)
			}
		} catch (e: VMDisconnectedException) {
			// Ignore since we wanted to stop the VM anyway
		}
	}
	
	override fun positionOf(location: Location): Position? = sourceOf(location)
		?.let { Position(it, location.lineNumber()) }
	
    private fun sourceOf(location: Location): Source? =
        try {
            val sourcePath = location.sourcePath()
            val sourceName = location.sourceName()

            sourcesRoots
                .asSequence()
                .map { it.resolve(sourcePath) }
                .orEmpty()
                .mapNotNull { findValidKtFilePath(it, sourceName) }
                .firstOrNull()
                ?.let { Source(
                    name = sourceName ?: it.fileName.toString(),
                    filePath = it
                ) }
        } catch(exception: AbsentInformationException) {
            null
        }
}
