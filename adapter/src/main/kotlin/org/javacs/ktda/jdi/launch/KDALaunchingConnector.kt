/*********************************************************************
 * Copyright (c) 2020 kotlin-debug-adapter and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.javacs.ktda.jdi.launch

import com.sun.jdi.VirtualMachine
import com.sun.jdi.connect.Connector
import com.sun.jdi.connect.Connector.Argument
import com.sun.jdi.connect.Connector.StringArgument
import com.sun.jdi.connect.IllegalConnectorArgumentsException
import com.sun.jdi.connect.LaunchingConnector
import com.sun.jdi.connect.VMStartException
import org.eclipse.jdi.internal.VirtualMachineImpl
import org.eclipse.jdi.internal.VirtualMachineManagerImpl
import org.eclipse.jdi.internal.connect.SocketLaunchingConnectorImpl
import org.eclipse.jdi.internal.connect.SocketListeningConnectorImpl
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

const val ARG_HOME = "home";
const val ARG_OPTIONS = "options";
const val ARG_MAIN = "main";
const val ARG_SUSPEND = "suspend";
const val ARG_QUOTE = "quote";
const val ARG_EXEC = "vmexec";
const val ARG_CWD = "cwd";
const val ARG_ENV = "envs";

const val ACCEPT_TIMEOUT = 10 * 1000

/**
 * A custom LaunchingConnector support cwd and env variables
 *
 * Inspired from AdvancedLaunchingConnector in java-debug:
 * https://github.com/microsoft/java-debug/blob/master/com.microsoft.java.debug.plugin/src/main/java/com/microsoft/java/debug/plugin/internal/AdvancedLaunchingConnector.java
 */
class KDALaunchingConnector(val virtualMachineManager: VirtualMachineManagerImpl) : SocketLaunchingConnectorImpl(virtualMachineManager), LaunchingConnector  {

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

    override fun defaultArguments(): Map<String, Argument>  {
        
        val defaultArgs = super.defaultArguments();
        defaultArgs[ARG_CWD] = StringArgumentImpl(ARG_CWD, "Current working directory", ARG_CWD);
        defaultArgs[ARG_ENV] = StringArgumentImpl(ARG_ENV, "Environment variables", ARG_ENV);

        return defaultArgs;
    }

    override fun name():String {
        return this.javaClass.name;
    }

    @Throws(IOException::class, IllegalConnectorArgumentsException::class, VMStartException::class)
    override fun launch(connectionArgs: Map<String, Argument>): VirtualMachine {
        val cwd = connectionArgs[ARG_CWD]?.value()
        var workingDir: File? = if (cwd != null && Files.isDirectory(Paths.get(cwd))) File(cwd) else null
        var envVars = urlDecode(connectionArgs[ARG_ENV]?.value())?.toTypedArray()

        val listenConnector = SocketListeningConnectorImpl(
                virtualMachineManager())
        val args = listenConnector.defaultArguments()

        (args["timeout"] as Connector.IntegerArgument?)!!.setValue(ACCEPT_TIMEOUT)

        val address = listenConnector.startListening(args)
        val cmds = constructLaunchCommand(connectionArgs, address)
        val process = Runtime.getRuntime().exec(cmds, envVars, workingDir)
        val vm: VirtualMachineImpl
        vm = try {
            listenConnector.accept(args) as VirtualMachineImpl
        } catch (e: IOException) {
            process.destroy()
            throw VMStartException(String.format("VM did not connect within given time: %d ms", ACCEPT_TIMEOUT), process)
        } catch (e: IllegalConnectorArgumentsException) {
            process.destroy()
            throw VMStartException(String.format("VM did not connect within given time: %d ms", ACCEPT_TIMEOUT), process)
        }
        vm.setLaunchedProcess(process)
        return vm
    }

    internal fun constructLaunchCommand( launchingOptions:Map<String, Argument>, address: String ): Array<String> {
        val javaHome = launchingOptions.get(ARG_HOME)?.value() ?: System.getenv("JAVA_HOME")
        val javaExec = launchingOptions.get(ARG_EXEC)?.value() ?: "java"
        val slash = System.getProperty("file.separator");
        val suspended = launchingOptions.get(ARG_SUSPEND)?.value()?.toBoolean() ?: true;
        val javaOptions = launchingOptions.get(ARG_OPTIONS)?.value();
        val main = launchingOptions.get(ARG_MAIN)?.value() ?: "";

        val execString: StringBuilder = StringBuilder();
        execString.append("\"" + javaHome + slash + "bin" + slash + javaExec + "\"");
        execString.append(" -Xdebug -Xnoagent -Djava.compiler=NONE");
        execString.append(" -agentlib:jdwp=transport=dt_socket,address=$address,server=n,suspend=" + if (suspended) "y" else "n")
        javaOptions?.let {
            execString.append(" $it")
        }
        execString.append(" $main")

        println("execString: ${execString.toString()}")

        return ArgumentsUtil.parseArguments(execString.toString()).toTypedArray();
    }

}

/**
 * Argument class for arguments that are used to establish a connection.
 */
class StringArgumentImpl constructor(private val fName: String, private val fDescription: String = "", private val fLabel: String = fName,
                                     private var fValue:String ?= null, private val fMustSpecify: Boolean = false) : StringArgument {

    /* (non-Javadoc)
		 * @see com.sun.jdi.connect.Connector.Argument#name()
		 */
    override fun name(): String {
        return fName
    }

    /* (non-Javadoc)
		 * @see com.sun.jdi.connect.Connector.Argument#description()
		 */
    override fun description(): String {
        return fDescription
    }

    /* (non-Javadoc)
		 * @see com.sun.jdi.connect.Connector.Argument#label()
		 */
    override fun label(): String {
        return fLabel
    }

    /* (non-Javadoc)
		 * @see com.sun.jdi.connect.Connector.Argument#mustSpecify()
		 */
    override fun mustSpecify(): Boolean {
        return fMustSpecify
    }

    /* (non-Javadoc)
		 * @see com.sun.jdi.connect.Connector.Argument#value()
		 */
    override fun value(): String? {
        return fValue
    }

    /* (non-Javadoc)
		 * @see com.sun.jdi.connect.Connector.Argument#setValue(java.lang.String)
		 */
    override fun setValue(value: String?){
        fValue = value
    }

    /* (non-Javadoc)
		 * @see com.sun.jdi.connect.Connector.Argument#isValid(java.lang.String)
		 */
    override fun isValid(value: String): Boolean{
        return true
    }
    override fun toString(): String {
        return fValue ?: ""
    }

    companion object {
        /**
         * Serial version id.
         */
        private const val serialVersionUID = 8850533280769854833L
    }

}

/**
 * A util class to parse arguments for different OS
 */
class ArgumentsUtil {
    companion object {

        /**
         * Parses the given command line into separate arguments that can be passed
         * to <code>Runtime.getRuntime().exec(cmdArray)</code>.
         *
         * @param cmdStr command line as a single string.
         * @return the individual arguments.
         */
        fun parseArguments(cmdStr: String?): List<String> {
            if (cmdStr == null) {
                return listOf()
            }
            return if(isWindows()) parseArgumentsWindows(cmdStr) else parseArgumentsNonWindows(cmdStr);
        }

        /**
         * Parses the given command line into separate arguments for mac/linux platform.
         * This piece of code is mainly copied from
         * https://github.com/eclipse/eclipse.platform.debug/blob/master/org.eclipse.debug.core/core/org/eclipse/debug/core/DebugPlugin.java#L1374
         *
         * @param args
         * the command line arguments as a single string.
         * @return the individual arguments
         */
        private fun parseArgumentsNonWindows(args: String): List<String> {
            // man sh, see topic QUOTING
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
                if (Character.isWhitespace(ch)) {
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
                    DEFAULT, ARG -> if (ch == '"') {
                        state = IN_DOUBLE_QUOTE
                    } else if (ch == '\'') {
                        state = IN_SINGLE_QUOTE
                    } else if (ch == '\\' && i + 1 < len) {
                        state = ARG
                        ch = args[++i]
                        buf.append(ch)
                    } else {
                        state = ARG
                        buf.append(ch)
                    }
                    IN_DOUBLE_QUOTE -> if (ch == '"') {
                        state = ARG
                    } else if (ch == '\\' && i + 1 < len && (args[i + 1] == '\\' || args[i + 1] == '"')) {
                        ch = args[++i]
                        buf.append(ch)
                    } else {
                        buf.append(ch)
                    }
                    IN_SINGLE_QUOTE -> if (ch == '\'') {
                        state = ARG
                    } else {
                        buf.append(ch)
                    }
                    else -> throw IllegalStateException()
                }
                i++
            }
            if (buf.isNotEmpty() || state != DEFAULT) {
                result.add(buf.toString())
            }
            return result
        }

        /**
         * Parses the given command line into separate arguments for windows platform.
         * This piece of code is mainly copied from
         * https://github.com/eclipse/eclipse.platform.debug/blob/master/org.eclipse.debug.core/core/org/eclipse/debug/core/DebugPlugin.java#L1264
         *
         * @param args
         * the command line arguments as a single string.
         * @return the individual arguments
         */
        private fun parseArgumentsWindows(args: String): List<String> {
            // see http://msdn.microsoft.com/en-us/library/a1y7w461.aspx
            val result = mutableListOf<String>()
            val DEFAULT = 0
            val ARG = 1
            val IN_DOUBLE_QUOTE = 2
            var state = DEFAULT
            var backslashes = 0
            val buf = java.lang.StringBuilder()
            val len = args.length
            var i = 0
            while (i < len) {
                val ch = args[i]
                if (ch == '\\') {
                    backslashes++
                    i++
                    continue
                } else if (backslashes != 0) {
                    if (ch == '"') {
                        while (backslashes >= 2) {
                            buf.append('\\')
                            backslashes -= 2
                        }
                        if (backslashes == 1) {
                            if (state == DEFAULT) {
                                state = ARG
                            }
                            buf.append('"')
                            backslashes = 0
                            i++
                            continue
                        } // else fall through to switch
                    } else {
                        // false alarm, treat passed backslashes literally...
                        if (state == DEFAULT) {
                            state = ARG
                        }
                        while (backslashes > 0) {
                            buf.append('\\')
                            backslashes--
                        }
                        // fall through to switch
                    }
                }
                if (Character.isWhitespace(ch)) {
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
                    DEFAULT, ARG -> if (ch == '"') {
                        state = IN_DOUBLE_QUOTE
                    } else {
                        state = ARG
                        buf.append(ch)
                    }
                    IN_DOUBLE_QUOTE -> if (ch == '"') {
                        if (i + 1 < len && args[i + 1] == '"') {
                            /* Undocumented feature in Windows:
                             * Two consecutive double quotes inside a double-quoted argument are interpreted as
                             * a single double quote.
                             */
                            buf.append('"')
                            i++
                        } else if (buf.isEmpty()) {
                            // empty string on Windows platform. Account for bug in constructor of JDK's java.lang.ProcessImpl.
                            result.add("\"\"") //$NON-NLS-1$
                            state = DEFAULT
                        } else {
                            state = ARG
                        }
                    } else {
                        buf.append(ch)
                    }
                    else -> throw IllegalStateException()
                }
                i++
            }
            if (buf.isNotEmpty() || state != DEFAULT) {
                result.add(buf.toString())
            }
            return result
        }

        fun isWindows() = System.getProperty("os.name", "").toLowerCase().contains("win")
    }
}
