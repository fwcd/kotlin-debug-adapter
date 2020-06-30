package org.javacs.ktda.jdi.launch

import com.sun.jdi.VirtualMachine
import com.sun.jdi.connect.Connector
import org.hamcrest.Matchers.*
import org.javacs.kt.LOG
import org.junit.Assert.assertThat
import org.junit.Test

class KDALaunchingConnectorTest {

    @Test
    fun `defaultArguments should include cwd and env`() {

        val manager = KDAVirtualMachineManager()

        val connector = KDALaunchingConnector(manager)

        val args = connector.defaultArguments()

        assertThat(args.size, greaterThanOrEqualTo(2))

        assertThat(args["cwd"], notNullValue())
        assertThat(args["envs"], notNullValue())
        assertThat(args["suspend"].toString(), equalTo("true"))

    }

    @Test
    fun `launch should succeed`() {

        val manager = KDAVirtualMachineManager()

        val connector = KDALaunchingConnector(manager)

        val connectionArgs: Map<String, Connector.Argument> = mapOf(
            "main" to StringArgumentImpl(ARG_MAIN, fValue = "", fMustSpecify = true),
            "options" to StringArgumentImpl(ARG_OPTIONS, fValue = "-version"),
            "suspend" to StringArgumentImpl(ARG_SUSPEND, fValue = "true")
        )

        var vm: VirtualMachine? = null

        vm = connector.launch(connectionArgs)
        assertThat(vm.name(), notNullValue())
        assertThat(vm.process(), notNullValue())

    }
}