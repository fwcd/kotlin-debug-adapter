package org.javacs.ktda.jdi.launch

import com.sun.jdi.Bootstrap
import com.sun.jdi.VirtualMachine
import com.sun.jdi.connect.*
import com.sun.jdi.connect.spi.Connection
import com.sun.jdi.connect.spi.TransportService
import org.codehaus.plexus.util.cli.CommandLineUtils
import org.javacs.kt.LOG
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal const val ARG_HOME = "home"
internal const val ARG_OPTIONS = "options"
internal const val ARG_MAIN = "main"
internal const val ARG_SUSPEND = "suspend"
internal const val ARG_QUOTE = "quote"
internal const val ARG_VM_EXEC = "vmexec"
internal const val ARG_CWD = "cwd"
internal const val ARG_ENVS = "envs"

/**
 * A custom LaunchingConnector that supports cwd and env variables
 */
open class KDACommandLineLauncher : LaunchingConnector {

    protected val defaultArguments = mutableMapOf<String, Connector.Argument>()

    /**
     * We only support SocketTransportService
     */
    protected var transportService : TransportService? = null
    protected var transport = Transport { "dt_socket" }

    companion object {

        fun urlEncode(arg: Collection<String>?) = arg
                ?.map { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) }
                ?.fold("") { a, b -> "$a\n$b" }

        fun urlDecode(arg: String?) = arg
                ?.trim('\n')
                ?.split("\n")
                ?.map { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                ?.toList()
    }

    constructor() : super() {

        defaultArguments[ARG_HOME] = StringArgument(ARG_HOME, description = "Java home", value = System.getProperty("java.home"))

        defaultArguments[ARG_OPTIONS] = StringArgument(ARG_OPTIONS, description = "Jvm arguments")

        defaultArguments[ARG_MAIN] = StringArgument(ARG_MAIN, description = "Main class name and parameters", mustSpecify = true)

        defaultArguments[ARG_SUSPEND] = StringArgument(ARG_SUSPEND, description = "Whether launch the debugee in suspend mode", value = "true")

        defaultArguments[ARG_QUOTE] = StringArgument(ARG_QUOTE, description = "Quote char", value = "\"")

        defaultArguments[ARG_VM_EXEC] = StringArgument(ARG_VM_EXEC, description = "The java exec", value = "java")

        defaultArguments[ARG_CWD] = StringArgument(ARG_CWD, description = "Current working directory")

        defaultArguments[ARG_ENVS] = StringArgument(ARG_ENVS, description = "Environment variables")

        // Load TransportService 's implementation
        transportService = Class.forName("com.sun.tools.jdi.SocketTransportService").getDeclaredConstructor().newInstance() as TransportService

        if(transportService == null){
            throw IllegalStateException("Failed to load com.sun.tools.jdi.SocketTransportService")
        }

    }

    override fun name(): String {
        return this.javaClass.name
    }

    override fun description(): String {
        return "A custom launcher supporting cwd and env variables"
    }

    override fun defaultArguments(): Map<String, Connector.Argument> {
        return this.defaultArguments
    }

    override fun toString(): String {
        return name()
    }

    override fun transport(): Transport {
        return transport
    }

    protected fun getOrDefault(arguments: Map<String, Connector.Argument>, argName: String): String {
        return arguments[argName]?.value() ?: defaultArguments[argName]?.value() ?: ""
    }

    /**
     * A customized method to launch the vm and connect to it, supporting cwd and env variables
     */
    @Throws(IOException::class, IllegalConnectorArgumentsException::class, VMStartException::class)
    override fun launch(arguments: Map<String, Connector.Argument>): VirtualMachine {
        val vm: VirtualMachine

        val home = getOrDefault(arguments, ARG_HOME)
        val options = getOrDefault(arguments, ARG_OPTIONS)
        val main = getOrDefault(arguments, ARG_MAIN)
        val suspend = getOrDefault(arguments, ARG_SUSPEND).toBoolean()
        val quote = getOrDefault(arguments, ARG_QUOTE)
        var exe = getOrDefault(arguments, ARG_VM_EXEC)
        val cwd = getOrDefault(arguments, ARG_CWD)
        val envs = urlDecode(getOrDefault(arguments, ARG_ENVS))?.toTypedArray()

        check(quote.length == 1) {"Invalid length for $ARG_QUOTE: $quote"}
        check(!options.contains("-Djava.compiler=") ||
                options.toLowerCase().contains("-djava.compiler=none")) { "Cannot debug with a JIT compiler. $ARG_OPTIONS: $options"}

        val listenKey = transportService?.startListening() ?: throw IllegalStateException("Failed to do transportService.startListening()")
        val address = listenKey.address()

        try {
            val command = StringBuilder()

            exe = if (home.isNotEmpty()) Paths.get(home, "bin", exe).toString() else exe
            command.append(wrapWhitespace(exe))

            command.append(" $options")

            //debug options
            command.append(" -agentlib:jdwp=transport=${transport.name()},address=$address,server=n,suspend=${if (suspend) 'y' else 'n'}")

            command.append(" $main")

            LOG.debug("command before tokenize: $command")

            vm = launch(commandArray = CommandLineUtils.translateCommandline(command.toString()), listenKey = listenKey,
                    ts = transportService!!, cwd = cwd, envs = envs
            )

        } finally {
            transportService?.stopListening(listenKey)
        }
        return vm
    }

    internal fun wrapWhitespace(str: String): String {
       return if(str.contains(' ')) "\"$str\" " else str
    }

    @Throws(IOException::class, VMStartException::class)
    fun launch(commandArray: Array<String>,
                        listenKey: TransportService.ListenKey,
                        ts: TransportService, cwd: String, envs: Array<String>? = null): VirtualMachine {

        val (connection, process) = launchAndConnect(commandArray, listenKey, ts, cwd = cwd, envs = envs)

        return Bootstrap.virtualMachineManager().createVirtualMachine(connection,
                process)
    }


    /**
     * launch the command, connect to transportService, and returns the connection / process pair
     */
    protected fun launchAndConnect(commandArray: Array<String>, listenKey: TransportService.ListenKey,
                         ts: TransportService, cwd: String = "", envs: Array<String>? = null): Pair<Connection, Process>{

        val dir = if(cwd.isNotBlank() && Files.isDirectory(Paths.get(cwd))) File(cwd) else null

        var threadCount = 0

        val executors = Executors.newFixedThreadPool(2) { Thread(it, "${this.javaClass.simpleName}-${threadCount++}") }
        val process = Runtime.getRuntime().exec(commandArray, envs, dir)

        val connectionTask: Callable<Any> = Callable { ts.accept(listenKey, 0,0).also { LOG.debug("ts.accept invoked") } }
        val exitCodeTask: Callable<Any> = Callable { process.waitFor().also { LOG.debug("process.waitFor invoked") } }

        try {
            when (val result = executors.invokeAny(listOf(connectionTask, exitCodeTask))) {
                // successfully connected to transport service
                is Connection -> return Pair(result, process)

                // cmd exited before connection. some thing wrong
                is Int -> throw VMStartException(
                        "VM initialization failed. exit code: ${process?.exitValue()}, cmd: $commandArray", process)

                // should never occur
                else -> throw IllegalStateException("Unknown result: $result")
            }
        } finally {
            // release the executors. no longer needed.
            executors.shutdown()
            executors.awaitTermination(1, TimeUnit.SECONDS)
        }

    }

}