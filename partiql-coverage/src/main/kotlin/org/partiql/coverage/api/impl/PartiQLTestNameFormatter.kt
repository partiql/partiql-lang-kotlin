package org.partiql.coverage.api.impl

import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.platform.commons.JUnitException
import org.junit.platform.commons.util.StringUtils
import java.lang.Exception
import java.text.MessageFormat
import java.util.Arrays
import java.util.stream.Collectors
import java.util.stream.IntStream

/**
 * @since 5.0
 */
internal class PartiQLTestNameFormatter(
    private val pattern: String,
    private val displayName: String,
    private val methodContext: PartiQLTestMethodContext,
    private val argumentMaxLength: Int
) {
    fun format(invocationIndex: Int, vararg arguments: Any): String {
        return try {
            formatSafely(invocationIndex, arguments)
        } catch (ex: Exception) {
            val message = ("The display name pattern defined for the PartiQLTest is invalid. "
                + "See nested exception for further details.")
            throw JUnitException(message, ex)
        }
    }

    private fun formatSafely(invocationIndex: Int, arguments: Array<out Any>): String {
        val namedArguments = extractNamedArguments(arguments)
        val pattern = prepareMessageFormatPattern(invocationIndex, namedArguments)
        val format = MessageFormat(pattern)
        val humanReadableArguments = makeReadable(format, namedArguments)
        val formatted = format.format(humanReadableArguments)
        return formatted.replace(TEMPORARY_DISPLAY_NAME_PLACEHOLDER, displayName)
    }

    private fun extractNamedArguments(arguments: Array<out Any>): Array<Any> {
        return Arrays.stream(arguments) //
            .map { argument: Any? -> if (argument is Named<*>) argument.name else argument } //
            .toArray()
    }

    private fun prepareMessageFormatPattern(invocationIndex: Int, arguments: Array<Any>): String {
        var result = pattern //
            .replace(ParameterizedTest.DISPLAY_NAME_PLACEHOLDER, TEMPORARY_DISPLAY_NAME_PLACEHOLDER) //
            .replace(ParameterizedTest.INDEX_PLACEHOLDER, invocationIndex.toString())
        if (result.contains(ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER)) {
            result =
                result.replace(ParameterizedTest.ARGUMENTS_WITH_NAMES_PLACEHOLDER, argumentsWithNamesPattern(arguments))
        }
        if (result.contains(ParameterizedTest.ARGUMENTS_PLACEHOLDER)) {
            result = result.replace(ParameterizedTest.ARGUMENTS_PLACEHOLDER, argumentsPattern(arguments))
        }
        return result
    }

    private fun argumentsWithNamesPattern(arguments: Array<Any>): String {
        return IntStream.range(0, arguments.size) //
            .mapToObj { index: Int ->
                (methodContext.getParameterName(index).map { name: String -> "$name=" }
                    .orElse("").toString() + "{"
                    + index + "}")
            } //
            .collect(Collectors.joining(", "))
    }

    private fun argumentsPattern(arguments: Array<Any>): String {
        return IntStream.range(0, arguments.size) //
            .mapToObj { index: Int -> "{$index}" } //
            .collect(Collectors.joining(", "))
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

    companion object {
        private const val ELLIPSIS = '\u2026'
        private const val TEMPORARY_DISPLAY_NAME_PLACEHOLDER = "~~~PARTIQL_DISPLAY_NAME~~~"
    }
}