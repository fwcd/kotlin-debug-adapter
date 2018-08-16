package com.fwcd.ktda

import com.fwcd.ktda.util.DelegatePrintStream
import java.time.Instant
import java.util.ArrayDeque
import java.util.Queue

/**
 * A static logger that will by bound to a backend later by KotlinDebugAdapter.
 */
val LOG = Logger()

enum class LogLevel(val level: Int) {
    NONE(100),
    ERROR(2),
    WARN(1),
    INFO(0),
    DEBUG(-1),
    TRACE(-2),
    DEEP_TRACE(-3),
    ALL(-100)
}

class Logger {
    private var outBackend: ((String) -> Unit)? = null
    private var errBackend: ((String) -> Unit)? = null
    private val outQueue: Queue<String> = ArrayDeque()
    private val errQueue: Queue<String> = ArrayDeque()
    
    private val newline = System.lineSeparator()
    val logTime = false
    var level = LogLevel.INFO
    
    private fun outputErrorLine(line: String) {
        if (errBackend == null) {
            errQueue.offer(line)
        } else {
            errBackend?.invoke(line)
        }
    }
    
    private fun outputLine(line: String) {
        if (outBackend == null) {
            outQueue.offer(line)
        } else {
            outBackend?.invoke(line)
        }
    }
    
    private fun log(msgLevel: LogLevel, msg: String, placeholders: Array<out Any>) {
        if (level <= msgLevel) {
            outputLine(format(msgLevel, insertPlaceholders(msg, placeholders)))
        }
    }
    
    fun error(throwable: Throwable) = throwable.printStackTrace(DelegatePrintStream(::outputErrorLine))
    
    fun error(msg: String, vararg placeholders: Any) = log(LogLevel.ERROR, msg, placeholders)
    
    fun warn(msg: String, vararg placeholders: Any) = log(LogLevel.WARN, msg, placeholders)
    
    fun info(msg: String, vararg placeholders: Any) = log(LogLevel.INFO, msg, placeholders)
    
    fun debug(msg: String, vararg placeholders: Any) = log(LogLevel.DEBUG, msg, placeholders)
    
    fun trace(msg: String, vararg placeholders: Any) = log(LogLevel.TRACE, msg, placeholders)
    
    fun deepTrace(msg: String, vararg placeholders: Any) = log(LogLevel.DEEP_TRACE, msg, placeholders)
    
    fun connectOutputBackend(outBackend: (String) -> Unit) {
        this.outBackend = outBackend
        flushOutQueue()
    }
    
    fun connectErrorBackend(errBackend: (String) -> Unit) {
        this.errBackend = errBackend
        flushErrQueue()
    }
    
    private fun insertPlaceholders(msg: String, placeholders: Array<out Any>): String {
        val msgLength = msg.length
        val lastIndex = msgLength - 1
        var charIndex = 0
        var placeholderIndex = 0
        var result = ""
        
        while (charIndex < msgLength) {
            val currentChar = msg.get(charIndex)
            val nextChar = if (charIndex != lastIndex) msg.get(charIndex + 1) else '?'
            if ((currentChar == '{') && (nextChar == '}')) {
                if (placeholderIndex >= placeholders.size) {
                    return "ERROR: Tried to log more '{}' placeholders than there are values"
                }
                result += placeholders[placeholderIndex]
                placeholderIndex += 1
                charIndex += 2
            } else {
                result += currentChar
                charIndex += 1
            }
        }
        
        return result
    }
    
    private fun flushOutQueue() {
        while (outQueue.isNotEmpty()) {
            outBackend?.invoke(outQueue.poll())
        }
    }
    
    private fun flushErrQueue() {
        while (errQueue.isNotEmpty()) {
            errBackend?.invoke(errQueue.poll())
        }
    }
    
    private fun format(level: LogLevel, message: String): String {
        val now = Instant.now()
        val time = if (logTime) "$now " else ""

        var thread = Thread.currentThread().name
        val prefix = "$time[${level.toString()}]"

        return multiLineFormat(
            2, // padding between columns
            FormatValue(prefix, 10),
            FormatValue(shortenOrPad(thread, 10)),
            FormatValue(message)
        )
    }

    data class FormatValue(val str: String, val charsPerLine: Int = str.length)

    private fun multiLineFormat(padding: Int, vararg values: FormatValue): String {
        val splittedValues = values.map { createLineBreaks(it.str, it.charsPerLine) }
        return mergeSplittedLines(splittedValues, padding)
    }

    private fun mergeSplittedLines(splittedValues: List<List<String>>, padding: Int): String {
        var charOffset = 0
        val lines = mutableListOf<String>()
        for (splittedValue in splittedValues) {
            var lineIndex = 0
            var maxOffset = 0
            for (valueLine in splittedValue) {
                while (lineIndex >= lines.size) lines.add("")

                lines[lineIndex] = lines[lineIndex].padEnd(charOffset, ' ') + valueLine

                maxOffset = Math.max(maxOffset, valueLine.length)
                lineIndex++
            }
            charOffset += maxOffset + padding
        }
        return lines.reduce { prev, current -> prev + newline + current } + newline
    }

    private fun createLineBreaks(str: String, maxLength: Int): List<String> {
        var current = ""
        var lines = mutableListOf<String>()
        var i = maxLength
        for (character in str) {
            val isNewline = character == '\n'
            if (i == 0 || isNewline) {
                lines.add(current.trim())
                current = ""
                i = maxLength
            }
            if (!isNewline) {
                current += character
            }
            i--
        }
        if (current.length > 0) lines.add(current.trim().padEnd(maxLength, ' '))
        return lines
    }

    private fun shortenOrPad(str: String, length: Int): String =
            if (str.length <= length) {
                str.padEnd(length, ' ')
            } else {
                ".." + str.substring(str.length - length + 2)
            }
}
