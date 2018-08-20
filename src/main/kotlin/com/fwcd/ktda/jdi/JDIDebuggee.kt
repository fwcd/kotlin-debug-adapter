package com.fwcd.ktda.jdi

import com.fwcd.ktda.core.Debuggee
import com.fwcd.ktda.core.DebuggeeThread
import com.fwcd.ktda.core.DebugContext
import com.fwcd.ktda.core.Position
import com.fwcd.ktda.core.Source
import com.fwcd.ktda.core.launch.LaunchConfiguration
import com.fwcd.ktda.core.event.DebuggeeEventBus
import com.fwcd.ktda.core.breakpoint.Breakpoint
import com.fwcd.ktda.core.breakpoint.ExceptionBreakpoint
import com.fwcd.ktda.LOG
import com.fwcd.ktda.util.ObservableList
import com.fwcd.ktda.util.KotlinDAException
import com.fwcd.ktda.classpath.findValidKtFilePath
import com.fwcd.ktda.classpath.toJVMClassNames
import com.fwcd.ktda.jdi.event.VMEventBus
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.Bootstrap
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.connect.Connector
import com.sun.jdi.event.ClassPrepareEvent
import com.sun.jdi.request.EventRequest
import com.sun.tools.jdi.SunCommandLineLauncher
import java.net.URLEncoder
import java.net.URLDecoder
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class JDIDebuggee(
	private val config: LaunchConfiguration,
	private val context: DebugContext,
	private val vmArguments: String? = null,
	private val modulePaths: String? = null,
	private val environmentVariables: Collection<String>? = null
): Debuggee, JDISessionContext {
	private val vm: VirtualMachine
	override val threads = ObservableList<DebuggeeThread>()
	override val eventBus: VMEventBus
	override val pendingStepRequestThreadIds = mutableSetOf<Long>()
	override val stdin: OutputStream?
	override val stdout: InputStream?
	override val stderr: InputStream?
	
	init {
		LOG.info("Starting JVM debug session with main class ${config.mainClass}")
		val vmManager = Bootstrap.virtualMachineManager()
		val connector = vmManager.launchingConnectors()
			.find { it is SunCommandLineLauncher }
			?: throw KotlinDAException("Could not find a launching connector (for a new debuggee VM)")
		val args: MutableMap<String, Connector.Argument> = connector.defaultArguments()
		
		args["suspend"]!!.setValue("true")
		args["options"]!!.setValue(formatOptions())
		args["main"]!!.setValue(formatMainClass())
		args["cwd"]?.setValue(config.rootPath.toAbsolutePath().toString())
		args["env"]?.setValue(urlEncode(environmentVariables) ?: "")
		
		vm = connector.launch(args)
		eventBus = VMEventBus(vm)
		
		val process = vm?.process()
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
		?.let(config.sourcesRoot::resolve)
		?.let(::findValidKtFilePath)
		?.let { Source(
			name = location.sourceName() ?: it.fileName.toString(),
			filePath = it
		) }
	
	private fun formatOptions(): String {
		var options = ""
		vmArguments?.let { options += it }
		modulePaths?.let { options += " --module-path \"$modulePaths\"" }
		options += " -classpath \"${formatClasspath()}\""
		return options
	}
	
	private fun formatMainClass(): String {
		val mainClasses = config.mainClass.split("/")
		return if ((modulePaths != null) || (mainClasses.size == 2)) {
			// Required for Java 9 compatibility
			"-m ${config.mainClass}"
		} else config.mainClass
	}
	
	private fun formatClasspath(): String = config.classpath
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
