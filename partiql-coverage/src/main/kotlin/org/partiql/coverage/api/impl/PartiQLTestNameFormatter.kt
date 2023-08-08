package org.partiql.coverage.api.impl

import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.platform.commons.JUnitException
import org.junit.platform.commons.util.StringUtils
import java.lang.Exception
import java.text.MessageFormat
import java.util.Arrays

internal object PartiQLTestNameFormatter {
    private const val pattern = "[{index}]"
    private const val argumentMaxLength: Int = 2
    private const val ELLIPSIS = '\u2026'

    fun format(invocationIndex: Int, vararg arguments: Any): String {
        return try {
            formatSafely(invocationIndex, arguments)
        } catch (ex: Exception) {
            val message = "The display name pattern defined for the PartiQLTest is invalid. See nested exception for further details."
            throw JUnitException(message, ex)
        }
    }

    private fun formatSafely(invocationIndex: Int, arguments: Array<out Any>): String {
        val namedArguments = extractNamedArguments(arguments)
        val pattern = prepareMessageFormatPattern(invocationIndex)
        val format = MessageFormat(pattern)
        val humanReadableArguments = makeReadable(format, namedArguments)
        return format.format(humanReadableArguments)
    }

    private fun extractNamedArguments(arguments: Array<out Any>): Array<Any> {
        return Arrays.stream(arguments) //
            .map { argument: Any? -> if (argument is Named<*>) argument.name else argument } //
            .toArray()
    }

    private fun prepareMessageFormatPattern(invocationIndex: Int): String {
        return pattern.replace(ParameterizedTest.INDEX_PLACEHOLDER, invocationIndex.toString())
    }

    private fun makeReadable(format: MessageFormat, arguments: Array<Any>): Array<Any?> {
        val formats = format.formatsByArgumentIndex
        val result = Arrays.copyOf(arguments, Math.min(arguments.size, formats.size), Array<Any>::class.java)
        for (i in result.indices) {
            if (formats[i] == null) {
                result[i] = truncateIfExceedsMaxLength(
                    StringUtils.nullSafeToString(
                        arguments[i]
                    )
                )
            }
        }
        return result
    }

    private fun truncateIfExceedsMaxLength(argument: String?): String? {
        return if (argument != null && argument.length > argumentMaxLength) {
            argument.substring(
                0,
                argumentMaxLength - 1
            ) + ELLIPSIS
        } else argument
    }
}