package org.javacs.ktda.core.exception

interface DebuggeeException {
    val description: String
    val message: String?
        get() = null
    val typeName: String?
        get() = null
    val fullTypeName: String?
        get() = null
    val stackTrace: String?
        get() = null
    val innerException: DebuggeeException?
        get() = null
}
