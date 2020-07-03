package org.javacs.ktda.jdi.launch

import org.hamcrest.Matchers.*
import org.junit.Assert.assertThat
import org.junit.Test

class KDACommandLineLauncherTest {

    @Test
    fun `defaultArguments should include cwd, envs, suspend`() {

        val connector = KDACommandLineLauncher()

        val args = connector.defaultArguments()

        assertThat(args.size, greaterThanOrEqualTo(2))

        assertThat(args["cwd"], notNullValue())
        assertThat(args["envs"], notNullValue())
        //suspend should default to true
        assertThat(args["suspend"]?.value(), equalTo("true"))

    }

}