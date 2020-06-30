package com.sun.tools.jdi

import com.sun.jdi.Bootstrap
import com.sun.jdi.VirtualMachine
import com.sun.jdi.connect.Connector
import com.sun.jdi.connect.IllegalConnectorArgumentsException
import com.sun.jdi.connect.VMStartException
import com.sun.jdi.connect.spi.Connection
import com.sun.jdi.connect.spi.TransportService
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

internal const val ARG_HOME = "home"
internal const val ARG_OPTIONS = "options"
internal const val ARG_MAIN = "main"
internal const val ARG_INIT_SUSPEND = "suspend"
internal const val ARG_QUOTE = "quote"
internal const val ARG_VM_EXEC = "vmexec"
internal const val ARG_CWD = "cwd"
internal const val ARG_ENVS = "envs"


class KDACommandLineLauncher : SunCommandLineLauncher {

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
        addStringArgument(
                ARG_CWD,
                ARG_CWD,
                "Current working directory",
                "",
                false)
        addStringArgument(
                ARG_ENVS,
                ARG_ENVS,
                "Environment variables",
                "",
                false)
    }

    override fun name(): String? {
        return this.javaClass.name
    }

    override fun description(): String? {
        return "A custom launcher supporting cwd and env variables"
    }

    /**
     * Copied from SunCommandLineLauncher.java and added cwd / env processing logic
     */
    @Throws(IOException::class, IllegalConnectorArgumentsException::class, VMStartException::class)
    override fun launch(arguments: Map<String, Connector.Argument>): VirtualMachine {
        val vm: VirtualMachine

        val home = argument(ARG_HOME, arguments).value()
        val options = argument(ARG_OPTIONS, arguments).value()
        val mainClassAndArgs = argument(ARG_MAIN, arguments).value()
        val wait = (argument(ARG_INIT_SUSPEND,
                arguments) as BooleanArgumentImpl).booleanValue()
        val quote = argument(ARG_QUOTE, arguments).value()
        val exe = argument(ARG_VM_EXEC, arguments).value()
        val cwd = argument(ARG_CWD, arguments).value()
        val envs = argument(ARG_ENVS, arguments).value()?.let { urlDecode(it) }?.toTypedArray()
        var exePath: String?
        if (quote.length > 1) {
            throw IllegalConnectorArgumentsException("Invalid length",
                    ARG_QUOTE)
        }
        if (options.indexOf("-Djava.compiler=") != -1 &&
                options.toLowerCase().indexOf("-djava.compiler=none") == -1) {
            throw IllegalConnectorArgumentsException("Cannot debug with a JIT compiler",
                    ARG_OPTIONS)
        }

        /*
         * Start listening.
         * If we're using the shared memory transport then we pick a
         * random address rather than using the (fixed) default.
         * Random() uses System.currentTimeMillis() as the seed
         * which can be a problem on windows (many calls to
         * currentTimeMillis can return the same value), so
         * we do a few retries if we get an IOException (we
         * assume the IOException is the filename is already in use.)
         */
        var listenKey: TransportService.ListenKey
        if (usingSharedMemory) {
            val rr = Random()
            var failCount = 0
            while (true) {
                try {
                    val address = "javadebug" + rr.nextInt(100000).toString()
                    listenKey = transportService().startListening(address)
                    break
                } catch (ioe: IOException) {
                    if (++failCount > 5) {
                        throw ioe
                    }
                }
            }
        } else {
            listenKey = transportService().startListening()
        }
        val address = listenKey.address()
        try {
            exePath = if (home.length > 0) {
                home + File.separator + "bin" + File.separator + exe
            } else {
                exe
            }
            // Quote only if necessary in case the quote arg value is bogus
            if (hasWhitespace(exePath)) {
                exePath = quote + exePath + quote
            }
            var xrun = "transport=" + transport().name() +
                    ",address=" + address +
                    ",suspend=" + if (wait) 'y' else 'n'
            // Quote only if necessary in case the quote arg value is bogus
            if (hasWhitespace(xrun)) {
                xrun = quote + xrun + quote
            }
            val command = exePath + ' ' +
                    options + ' ' +
                    "-Xdebug " +
                    "-Xrunjdwp:" + xrun + ' ' +
                    mainClassAndArgs

            vm = launch(commandArray = tokenizeCommand(command, quote[0]), listenKey = listenKey,
                    ts = transportService(), cwd = cwd, envs = envs, grp = grp
            )
        } finally {
            transportService().stopListening(listenKey)
        }
        return vm
    }

    @Throws(IOException::class, VMStartException::class)
    fun launch(commandArray: Array<String>,
                        listenKey: TransportService.ListenKey,
                        ts: TransportService, cwd: String?, envs: Array<String>? = null, grp: ThreadGroup): VirtualMachine {
        val helper = Helper(commandArray, listenKey, ts, cwd = cwd, envs = envs, grp = grp)
        helper.launchAndAccept()
        val manager = Bootstrap.virtualMachineManager()
        return manager.createVirtualMachine(helper.connection(),
                helper.process())
    }

    /**
     *
     * Copied from com.sun.tools.jdi.AbstractLauncher.Helper. Add cwd support.
     *
     * This class simply provides a context for a single launch and
     * accept. It provides instance fields that can be used by
     * all threads involved. This stuff can't be in the Connector proper
     * because the connector is a singleton and is not specific to any
     * one launch.
     */
    class Helper internal constructor(private val commandArray: Array<String>, private val listenKey: TransportService.ListenKey,
                                      private val ts: TransportService, private val cwd: String? = null, private val envs: Array<String>? = null, private val grp: ThreadGroup) {
        private var process: Process? = null
        private var connection: Connection? = null
        private var acceptException: IOException? = null
        private var exited = false

        /**
         * for wait()/notify()
         */
        private val lock: java.lang.Object = Object()

        fun commandString(): String {
            var str = ""
            for (i in commandArray.indices) {
                if (i > 0) {
                    str += " "
                }
                str += commandArray[i]
            }
            return str
        }

        @Throws(IOException::class, VMStartException::class)
        fun launchAndAccept() {
            synchronized(lock) {
                process = Runtime.getRuntime().exec(commandArray, envs, cwd?.let { File(it) })
                val acceptingThread = acceptConnection()
                val monitoringThread = monitorTarget()
                try {
                    while (connection == null &&
                            acceptException == null &&
                            !exited) {
                        lock.wait()
                    }
                    if (exited) {
                        throw VMStartException(
                                "VM initialization failed for: " + commandString(), process)
                    }
                    if (acceptException != null) {
                        // Rethrow the exception in this thread
                        throw acceptException ?: IOException("acceptException")
                    }
                } catch (e: InterruptedException) {
                    throw InterruptedIOException("Interrupted during accept")
                } finally {
                    acceptingThread.interrupt()
                    monitoringThread.interrupt()
                }
            }
        }

        fun process(): Process? {
            return process
        }

        fun connection(): Connection? {
            return connection
        }

        fun notifyOfExit() {
            synchronized(lock) {
                exited = true
                lock.notify()
            }
        }

        fun notifyOfConnection(connection: Connection?) {
            synchronized(lock) {
                this.connection = connection
                lock.notify()
            }
        }

        fun notifyOfAcceptException(acceptException: IOException?) {
            synchronized(lock) {
                this.acceptException = acceptException
                lock.notify()
            }
        }

        fun monitorTarget(): Thread {
            val thread: Thread = object : Thread(grp,
                    "launched target monitor") {
                override fun run() {
                    try {
                        process!!.waitFor()
                        /*
                             * Notify waiting thread of VM error termination
                             */notifyOfExit()
                    } catch (e: InterruptedException) {
                        // Connection has been established, stop monitoring
                    }
                }
            }
            thread.isDaemon = true
            thread.start()
            return thread
        }

        fun acceptConnection(): Thread {
            val thread: Thread = object : Thread(grp,
                    "connection acceptor") {
                override fun run() {
                    try {
                        val connection = ts.accept(listenKey, 0, 0)
                        /*
                             * Notify waiting thread of connection
                             */notifyOfConnection(connection)
                    } catch (e: InterruptedIOException) {
                        // VM terminated, stop accepting
                    } catch (e: IOException) {
                        // Report any other exception to waiting thread
                        notifyOfAcceptException(e)
                    }
                }
            }
            thread.isDaemon = true
            thread.start()
            return thread
        }

    }
}