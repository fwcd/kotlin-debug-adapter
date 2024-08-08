package org.javacs.ktda.jdi.launch

import com.sun.jdi.Bootstrap
import com.sun.jdi.VirtualMachine
import com.sun.jdi.connect.*
import com.sun.jdi.connect.spi.Connection
import com.sun.jdi.connect.spi.TransportService
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.javacs.kt.LOG

internal const val ARG_HOME = "home"
internal const val ARG_OPTIONS = "options"
internal const val ARG_MAIN = "main"
internal const val ARG_SUSPEND = "suspend"
internal const val ARG_QUOTE = "quote"
internal const val ARG_VM_EXEC = "vmexec"
internal const val ARG_CWD = "cwd"
internal const val ARG_ENV = "env"

/** A custom LaunchingConnector that supports cwd and env variables */
open class KDACommandLineLauncher : LaunchingConnector {

    protected val defaultArguments = mutableMapOf<String, Connector.Argument>()

    /** We only support SocketTransportService */
    protected val transportService: TransportService
    protected val transport = Transport { "dt_socket" }

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
        defaultArguments.apply {
            set(ARG_HOME, StringArgument(ARG_HOME, "Java home", value = System.getProperty("java.home")))
            set(ARG_OPTIONS, StringArgument(ARG_OPTIONS, "Jvm arguments"))
            set(ARG_MAIN, StringArgument(ARG_MAIN, "Main class name and parameters", mustSpecify = true))
            set(ARG_SUSPEND, StringArgument(ARG_SUSPEND, "Whether launch the debugee in suspend mode", value = "true"))
            set(ARG_QUOTE, StringArgument(ARG_QUOTE, "Quote char", value = "\""))
            set(ARG_VM_EXEC, StringArgument(ARG_VM_EXEC, "The java exec", value = "java"))
            set(ARG_CWD, StringArgument(ARG_CWD, "Current working directory"))
            set(ARG_ENV, StringArgument(ARG_ENV, "Environment variables"))
        }

        // Load TransportService 's implementation
        try {
            transportService =
                    Class.forName("com.sun.tools.jdi.SocketTransportService")
                            .getDeclaredConstructor()
                            .newInstance() as
                            TransportService
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load com.sun.tools.jdi.SocketTransportService")
        }
    }

    override fun name(): String = javaClass.name

    override fun description(): String = "A custom launcher supporting cwd and env variables"

    override fun defaultArguments(): Map<String, Connector.Argument> = defaultArguments

    override fun toString(): String = name()

    override fun transport(): Transport = transport

    protected fun getOrDefault(
        arguments: Map<String, Connector.Argument>,
        argName: String
    ): String {
        return arguments[argName]?.value() ?: defaultArguments[argName]?.value() ?: ""
    }

    private fun tokenizeCommandLine(args: String): Array<String> {
        val result = mutableListOf<String>()
    
        val DEFAULT = 0
        val ARG = 1
        val IN_DOUBLE_QUOTE = 2
        val IN_SINGLE_QUOTE = 3
    
        var state = DEFAULT
        val buf = StringBuilder()
        val len = args.length
        var i = 0
    
        while (i < len) {
            var ch = args[i]
            if (ch.isWhitespace()) {
                if (state == DEFAULT) {
                    // skip
                    i++
                    continue
                } else if (state == ARG) {
                    state = DEFAULT
                    result.add(buf.toString())
                    buf.setLength(0)
                    i++
                    continue
                }
            }
            when (state) {
                DEFAULT, ARG -> {
                    when {
                        ch == '"' -> state = IN_DOUBLE_QUOTE
                        ch == '\'' -> state = IN_SINGLE_QUOTE
                        ch == '\\' && i + 1 < len -> {
                            state = ARG
                            ch = args[++i]
                            buf.append(ch)
                        }
                        else -> {
                            state = ARG
                            buf.append(ch)
                        }
                    }
                }
                IN_DOUBLE_QUOTE -> {
                    when {
                        ch == '"' -> state = ARG
                        ch == '\\' && i + 1 < len && (args[i + 1] == '\\' || args[i + 1] == '"') -> {
                            ch = args[++i]
                            buf.append(ch)
                        }
                        else -> buf.append(ch)
                    }
                }
                IN_SINGLE_QUOTE -> {
                    if (ch == '\'') {
                        state = ARG
                    } else {
                        buf.append(ch)
                    }
                }
                else -> throw IllegalStateException()
            }
            i++
        }
        if (buf.isNotEmpty() || state != DEFAULT) {
            result.add(buf.toString())
        }
    
        return result.toTypedArray()
    }

    private fun buildCommandLine(arguments: Map<String, Connector.Argument>, address: String): String {
        val home = getOrDefault(arguments, ARG_HOME)
        val javaExe = getOrDefault(arguments, ARG_VM_EXEC)
        val options = getOrDefault(arguments, ARG_OPTIONS)
        val suspend = getOrDefault(arguments, ARG_SUSPEND).toBoolean()
        val main = getOrDefault(arguments, ARG_MAIN)
        
        val exe = if (home.isNotEmpty()) Paths.get(home, "bin", javaExe).toString() else javaExe

        return StringBuilder().apply {
            append("$exe")
            val jdwpLine = arrayOf(
                "transport=${transport.name()}",
                "address=$address",
                "server=n",
                "suspend=${if (suspend) 'y' else 'n'}"
            )
            append(" -agentlib:jdwp=${jdwpLine.joinToString(",")}")
            append(" $options")
            append(" $main")
        }.toString()
    }

    /** A customized method to launch the vm and connect to it, supporting cwd and env variables */
    @Throws(IOException::class, IllegalConnectorArgumentsException::class, VMStartException::class)
    override fun launch(arguments: Map<String, Connector.Argument>): VirtualMachine {        
        val quote = getOrDefault(arguments, ARG_QUOTE)
        val options = getOrDefault(arguments, ARG_OPTIONS)
        val cwd = getOrDefault(arguments, ARG_CWD)
        val env = urlDecode(getOrDefault(arguments, ARG_ENV))?.toTypedArray()

        check(quote.length == 1) { "Invalid length for $ARG_QUOTE: $quote" }
        check(!options.contains("-Djava.compiler=") || options.lowercase().contains("-djava.compiler=none")) {
            "Cannot debug with a JIT compiler. $ARG_OPTIONS: $options" 
        }

        val listenKey = transportService.startListening()
        ?: throw IllegalStateException("Failed to do transportService.startListening()")

        val command = buildCommandLine(arguments, listenKey.address())
        val tokenizedCommand = tokenizeCommandLine(command)

        val (connection, process) = launchAndConnect(tokenizedCommand, listenKey, transportService, cwd, env)
        return Bootstrap.virtualMachineManager().createVirtualMachine(connection, process)
    }

    private fun streamToString(stream: InputStream): String {
        val lineSep = System.getProperty("line.separator")
        return StringBuilder().apply {
            var line: String?
            val reader = stream.bufferedReader()

            while (reader.readLine().also { line = it } != null) {
                append(line)
                append(lineSep)
            }
        }.toString()
    }

    /**
     * launch the command, connect to transportService, and returns the connection / process pair
     */
    protected fun launchAndConnect(
            commandArray: Array<String>,
            listenKey: TransportService.ListenKey,
            ts: TransportService,
            cwd: String = "",
            envs: Array<String>? = null
    ): Pair<Connection, Process> {

        val dir = if (cwd.isNotBlank() && Files.isDirectory(Paths.get(cwd))) File(cwd) else null

        val process = Runtime.getRuntime().exec(commandArray, envs, dir)

        val result = CompletableFuture<Pair<Connection, Process>>()

        process.onExit().thenAcceptAsync() { pr ->
            if (result.isDone) return@thenAcceptAsync

            val exitCode = pr.exitValue()
            val stdOut = streamToString(process.inputStream)
            val stdErr = streamToString(process.errorStream)

            LOG.error("Process exited with status: ${exitCode} \n $stdOut \n $stdErr")
            result.completeExceptionally(VMStartException("Process exited with status: ${exitCode}", process))

            try {
                ts.stopListening(listenKey)
            } catch (e: Exception) {}
        }

        CompletableFuture.runAsync() {
            try {
                val vm = ts.accept(listenKey, 5_000, 5_000)
                result.complete(Pair(vm, process))
            } catch (e: IllegalConnectorArgumentsException) {
                result.completeExceptionally(e)
            } catch (e: IOException) {
                if (result.isDone) return@runAsync

                val stdOut = streamToString(process.inputStream)
                val stdErr = streamToString(process.errorStream)

                LOG.error("Failed to connect to the launched process: \n $stdOut \n $stdErr")

                process.destroy()

                result.completeExceptionally(e)
            } catch (e: RuntimeException) {
                result.completeExceptionally(e)
            }
        }

        return result.get()
    }
}
