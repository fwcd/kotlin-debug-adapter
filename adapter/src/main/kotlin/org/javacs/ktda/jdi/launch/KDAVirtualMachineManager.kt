package org.javacs.ktda.jdi.launch

import com.sun.jdi.VirtualMachineManager
import com.sun.jdi.connect.LaunchingConnector
import org.eclipse.jdi.internal.VirtualMachineManagerImpl

/**
 * A custom virtualMachineManager which add KDALaunchingConnector in the first place
 */
class KDAVirtualMachineManager : VirtualMachineManagerImpl(), VirtualMachineManager {

    companion object {
        private val manager =  KDAVirtualMachineManager()

        fun manager(): VirtualMachineManager {
            return manager
        }
    }

    override fun launchingConnectors(): List<LaunchingConnector> {
        return mutableListOf<LaunchingConnector>(KDALaunchingConnector(this))
                .also { it.addAll(super.launchingConnectors()) }
    }
}
