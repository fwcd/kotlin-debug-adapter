package org.javacs.ktda.jdi.launch

import com.sun.jdi.connect.Connector

/**
 * An implementation to Connector.Argument, used for arguments to launch a LaunchingConnector
 */
class StringArgument constructor(private val name: String, private val description: String = "", private val label: String = name,
                                     private var value:String = "", private val mustSpecify: Boolean = false) : Connector.Argument {

    override fun name(): String {
        return name
    }

    override fun description(): String {
        return description
    }

    override fun label(): String {
        return label
    }

    override fun mustSpecify(): Boolean {
        return mustSpecify
    }

    override fun value(): String {
        return value
    }

    override fun setValue(value: String){
        this.value = value
    }

    override fun isValid(value: String): Boolean{
        return true
    }
    override fun toString(): String {
        return value
    }


}