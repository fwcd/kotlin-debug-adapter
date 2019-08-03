package fwcd.ktda.jdi

import fwcd.ktda.core.Debuggee
import fwcd.ktda.core.DebuggeeThread
import fwcd.ktda.core.DebugContext
import fwcd.ktda.core.Position
import fwcd.ktda.core.Source
import fwcd.ktda.core.launch.LaunchConfiguration
import fwcd.ktda.core.event.DebuggeeEventBus
import fwcd.ktda.core.breakpoint.Breakpoint
import fwcd.ktda.core.breakpoint.ExceptionBreakpoint
import fwcd.ktda.LOG
import fwcd.ktda.util.ObservableList
import fwcd.ktda.classpath.findValidKtFilePath
import fwcd.ktda.classpath.toJVMClassNames
import fwcd.ktda.jdi.event.VMEventBus
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.request.EventRequest
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.nio.charset.StandardCharsets

class JDIDebuggee(
	private val vm: VirtualMachine,
	private val sourcesRoot: Path,
	private val context: DebugContext
) : Debuggee, JDISessionContext {
	override val threads = ObservableList<DebuggeeThread>()
	override val eventBus: VMEventBus
	override val pendingStepRequestThreadIds = mutableSetOf<Long>()
	override val stdin: OutputStream?
	override val stdout: InputStream?
	override val stderr: InputStream?
	
	init {
		eventBus = VMEventBus(vm)
		
		val process = vm.process()
		stdin = process?.outputStream
		stdout = process?.inputStream
		stderr = process?.errorStream
		
		updateThreads()
		hookBreakpoints()
	}
	
	private fun updateThreads() = threads.setAll(vm.allThreads().map { JDIThread(it, this) })
	
	private fun hookBreakpoints() {
		context.breakpointManager.also { manager ->
			manager.breakpoints.listenAndFire { setAllBreakpoints(it.values.flatten()) }
			manager.exceptionBreakpoints.listenAndFire(::setExceptionBreakpoints)
		}
	}
	
	private fun setAllBreakpoints(breakpoints: List<Breakpoint>) {
		vm.eventRequestManager().deleteAllBreakpoints()
		breakpoints.forEach { bp ->
			bp.position.let { setBreakpoint(
				it.source.filePath.toAbsolutePath().toString(),
				it.lineNumber
			) }
		}
	}
	
	private fun setExceptionBreakpoints(breakpoints: Set<ExceptionBreakpoint>) = vm
		.eventRequestManager()
		.createExceptionRequest(
			null,
			breakpoints.contains(ExceptionBreakpoint.CAUGHT),
			breakpoints.contains(ExceptionBreakpoint.UNCAUGHT)
		)
		.apply { setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD) }
		.enable()
	
	/** Tries to set a breakpoint */
	private fun setBreakpoint(filePath: String, lineNumber: Long) {
		val eventRequestManager = vm.eventRequestManager()
		toJVMClassNames(filePath)
			.forEach { className ->
				// Try setting breakpoint using a ClassPrepareRequest
				
				eventRequestManager
					.createClassPrepareRequest()
					.apply { addClassFilter(className) } // For global types
					.enable()
				eventRequestManager
					.createClassPrepareRequest()
					.apply { addClassFilter(className + "$*") } // For local types
					.enable()
				
				eventBus.subscribe(ClassPrepareEvent::class) {
					setBreakpointAtType(it.jdiEvent.referenceType(), lineNumber)
				}
				
				// Try setting breakpoint using loaded VM classes
				
				vm.classesByName(className).forEach {
					setBreakpointAtType(it, lineNumber)
				}
			}
	}
	
	/** Tries to set a breakpoint - Will return whether this was successful */
	private fun setBreakpointAtType(refType: ReferenceType, lineNumber: Long): Boolean {
		val location = refType
			.locationsOfLine(lineNumber.toInt())
			?.firstOrNull() ?: return false
		val request = vm.eventRequestManager()
			.createBreakpointRequest(location)
		request?.let {
			it.enable()
		}
		return request != null
	}
	
	override fun stop() {
		LOG.info("Stopping JDI session")
		if (vm.process()?.isAlive() ?: true) {
			vm.exit(0)
		}
	}
	
	override fun positionOf(location: Location): Position? = extractSource(location)
		?.let { Position(it, location.lineNumber().toLong()) }
	
	private fun extractSource(location: Location): Source? = location.sourcePath()
		?.let(sourcesRoot::resolve)
		?.let(::findValidKtFilePath)
		?.let { Source(
			name = location.sourceName() ?: it.fileName.toString(),
			filePath = it
		) }
}
