package com.fwcd.ktda.jdi

import com.fwcd.ktda.core.Debuggee
import com.fwcd.ktda.core.DebuggeeThread
import com.fwcd.ktda.core.DebugContext
import com.fwcd.ktda.core.Position
import com.fwcd.ktda.core.Source
import com.fwcd.ktda.core.launch.LaunchConfiguration
import com.fwcd.ktda.core.event.DebuggeeEventBus
import com.fwcd.ktda.LOG
import com.fwcd.ktda.util.ObservableList
import com.fwcd.ktda.util.KotlinDAException
import com.fwcd.ktda.classpath.findValidKtFilePath
import com.fwcd.ktda.jdi.event.VMEventBus
import com.sun.jdi.Location
import com.sun.jdi.Bootstrap
import com.sun.jdi.VirtualMachine
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.connect.Connector
import com.sun.tools.jdi.SunCommandLineLauncher
import java.net.URLEncoder
import java.net.URLDecoder
import java.io.File
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
		
		updateThreads()
	}
	
	private fun updateThreads() = threads.setAll(vm.allThreads().map { JDIThread(it, this) })
	
	override fun stop() {
		LOG.info("Stopping JDI session")
		if (vm.process()?.isAlive() ?: true) {
			vm.exit(0)
		}
	}
	
	override fun positionOf(location: Location): Position? = extractSource(location)
		?.let { Position(it, location.lineNumber()) }
	
	private fun extractSource(location: Location): Source? = location.sourcePath()
		?.let(config.sourcesRoot::resolve)
		?.let(::findValidKtFilePath)
		?.let(::Source)
	
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
