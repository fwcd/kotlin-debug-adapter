package org.javacs.ktda.jdi.exception

import com.sun.jdi.ObjectReference
import com.sun.jdi.ThreadReference
import org.javacs.ktda.core.exception.DebuggeeException

class JDIException(
    private val exception: ObjectReference,
    private val thread: ThreadReference
) : DebuggeeException {
    private val type by lazy { exception.referenceType() }

    override val fullTypeName: String by lazy { type.name() }
    override val typeName: String? by lazy { fullTypeName.split(".").last() }
    override val description: String by lazy {
        type.methodsByName("toString")
            .firstOrNull()
            ?.let { exception.invokeMethod(thread, it, emptyList(), 0) }
            ?.toString()
            ?: fullTypeName
    }
    override val message: String? by lazy {
        type.methodsByName("getMessage")
            .firstOrNull()
            ?.let { exception.invokeMethod(thread, it, emptyList(), 0) }
            ?.toString()
    }
    override val innerException: JDIException? by lazy {
        type.methodsByName("getCause")
            .firstOrNull()
            ?.let { exception.invokeMethod(thread, it, emptyList(), 0)?.let { it as? ObjectReference } }
            ?.let { JDIException(it, thread) }
    }

    // TODO: Stack frames
}
