package com.fwcd.ktda.jdi

import com.fwcd.ktda.LOG
import com.fwcd.ktda.util.KotlinDAException
import com.fwcd.ktda.util.ListenerList
import com.fwcd.ktda.util.Subscription
import com.fwcd.ktda.classpath.toJVMClassNames
import com.fwcd.ktda.adapter.BreakpointManager
import com.fwcd.ktda.core.launch.LaunchConfiguration
import com.fwcd.ktda.jdi.event.VMEventBus
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.io.File
import java.net.URLEncoder
import java.net.URLDecoder
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass
import org.eclipse.lsp4j.debug.Breakpoint
import com.sun.jdi.Bootstrap
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.event.Event
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.StepEvent
import com.sun.jdi.request.StepRequest
import com.sun.jdi.request.EventRequest
import com.sun.jdi.request.ClassPrepareRequest
import com.sun.jdi.request.BreakpointRequest
import com.sun.jdi.Location
import com.sun.jdi.connect.Connector
import com.sun.jdi.ReferenceType
import com.sun.tools.jdi.SunCommandLineLauncher

/**
 * The debugging backend that uses the Java Debug Interface.
 */
@Deprecated("Migration to the new architecture")
class JVMDebugSession(
	private val project: LaunchConfiguration,
	private val breakpointManager: BreakpointManager,
	private val vmArguments: String? = null,
	private val modulePaths: String? = null,
	private val environmentVariables: Collection<String>? = null
) {
	private val vm: VirtualMachine
	val vmEvents: VMEventBus
	val pendingStepRequestThreadIds = mutableSetOf<Long>()
	
	init {
		LOG.info("Starting JVM debug session with main class ${project.mainClass}")
		val vmManager = Bootstrap.virtualMachineManager()
		val connector = vmManager.launchingConnectors()
			.find { it is SunCommandLineLauncher }
			?: throw KotlinDAException("Could not find a launching connector (for a new debuggee VM)")
		val args: MutableMap<String, Connector.Argument> = connector.defaultArguments()
		
		args["suspend"]!!.setValue("true")
		args["options"]!!.setValue(formatOptions())
		args["main"]!!.setValue(formatMainClass())
		args["cwd"]?.setValue(project.rootPath.toAbsolutePath().toString())
		args["env"]?.setValue(urlEncode(environmentVariables) ?: "")
		
		vm = connector.launch(args)
		vmEvents = VMEventBus(vm)
		
		hookBreakpoints()
	}
	
	private fun hookBreakpoints() {
		breakpointManager.listeners.add(::setAllBreakpoints)
		setAllBreakpoints(breakpointManager.allBreakpoints())
	}
	
	fun stop() {
		LOG.info("Stopping JVM debug session")
		if (vm.process()?.isAlive() ?: true) {
			vm.exit(0)
		}
	}
	
	fun pauseThread(threadId: Long) = threadNr(threadId)?.let { thread ->
		if (!thread.isSuspended()) {
			thread.suspend()
		}
	}
	
	fun resumeThread(threadId: Long) = threadNr(threadId)?.let { thread ->
		(0 until thread.suspendCount()).forEach {
			thread.resume()
		}
	}
	
	// TODO: Cache stack traces?
	fun stackTrace(threadId: Long) = threadNr(threadId)?.frames()
	
	fun stepOver(threadId: Long) = stepLine(threadId, StepRequest.STEP_OVER)
	
	fun stepInto(threadId: Long) = stepLine(threadId, StepRequest.STEP_INTO)
	
	fun stepOut(threadId: Long) = stepLine(threadId, StepRequest.STEP_OUT)
	
	private fun stepLine(threadId: Long, depth: Int) = stepRequest(threadId, StepRequest.STEP_LINE, depth)
		?.let { performStep(threadId, it) }
	
	private fun performStep(threadId: Long, request: StepRequest) {
		request.enable()
		resumeThread(threadId)
	}
	
	private fun stepRequest(threadId: Long, size: Int, depth: Int) =
		if (pendingStepRequestThreadIds.contains(threadId)) null else {
			threadNr(threadId)
				?.let {
					it.virtualMachine()
						.eventRequestManager()
						.createStepRequest(it, size, depth)
				}?.also { request ->
					request.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD)
					request.addCountFilter(1)
					
					// Abort pending StepRequest when a breakpoint is hit
					pendingStepRequestThreadIds.add(threadId)
					
					fun abortUponEvent(eventClass: KClass<out Event>) {
						var sub: Subscription? = null
						
						sub = vmEvents.subscribe(eventClass) {
							val pending = pendingStepRequestThreadIds.contains(threadId)
							if (pending) {
								vm.eventRequestManager().deleteEventRequest(request)
								pendingStepRequestThreadIds.remove(threadId)
							}
							sub?.unsubscribe()
						}
					}
					
					abortUponEvent(StepEvent::class)
					abortUponEvent(BreakpointEvent::class)
				}
		}
	
	private fun threadNr(id: Long) = allThreads()
		.filter { (it.uniqueID() == id) && (!it.isCollected()) }
		.firstOrNull()
	
	fun allThreads() = try {
			vm.allThreads()
		} catch (e: VMDisconnectedException) {
			null
		}.orEmpty()
	
	private fun setAllBreakpoints(breakpoints: List<Breakpoint>) {
		vm.eventRequestManager().deleteAllBreakpoints()
		breakpoints.forEach { bp ->
			bp.source.path
				?.let { setBreakpoint(it, bp.line) }
			// TODO: Deal with the case where Breakpoint.source.path is absent
		}
	}
	
	/** Tries to set a breakpoint */
	private fun setBreakpoint(filePath: String, lineNumber: Long) {
		toJVMClassNames(filePath)
			.forEach { className ->
				// Try setting breakpoint using a ClassPrepareRequest
				
				vm.eventRequestManager()
					.createClassPrepareRequest()
					.apply { addClassFilter(className) } // For global types
					.enable()
				vm.eventRequestManager()
					.createClassPrepareRequest()
					.apply { addClassFilter(className + "$*") } // For local types
					.enable()
				
				vmEvents.subscribe(ClassPrepareEvent::class) {
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
	
	private fun formatOptions(): String {
		var options = ""
		vmArguments?.let { options += it }
		modulePaths?.let { options += " --module-path \"$modulePaths\"" }
		options += " -classpath \"${formatClasspath()}\""
		return options
	}
	
	private fun formatMainClass(): String {
		val mainClasses = project.mainClass.split("/")
		return if ((modulePaths != null) || (mainClasses.size == 2)) {
			// Required for Java 9 compatibility
			"-m ${project.mainClass}"
		} else project.mainClass
	}
	
	private fun formatClasspath(): String = project.classpath
		.map { it.toAbsolutePath().toString() }
		.reduce { prev, next -> "$prev${File.pathSeparatorChar}$next" }
		
	private fun urlEncode(arg: Collection<String>?) = arg
		?.map { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }
		?.reduce { a, b -> "$a\n$b" }
		
	private fun urlDecode(arg: String?) = arg
		?.split("\n")
		?.map { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
		?.toList()
}
