package org.partiql.cli.pipeline

import org.partiql.cli.ErrorCodeString
import org.partiql.cli.shell.error
import org.partiql.cli.shell.warn
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.Error
import org.partiql.spi.errors.ErrorCode
import org.partiql.spi.errors.ErrorListener
import org.partiql.spi.errors.ErrorListenerException
import org.partiql.spi.errors.Property
import org.partiql.spi.function.Function
import java.io.PrintStream

class AppErrorListener(
    private val out: PrintStream,
    private val maxErrors: Int,
    private val inhibitWarnings: Boolean,
    warningsAsErrors: Array<ErrorCodeString>
) : ErrorListener {

    private val _warningsAsErrors = warningsAsErrors.map { it.code }.toSet()
    private var _errorCount: Int = 0
    private var component: String = "Internal"
    private val allWarningsAsErrors: Boolean = _warningsAsErrors.contains(ErrorCodeString.ALL.code)
    private val _verbose = true

    fun setComponent(component: String) {
        this.component = component.lowercase()
    }

    override fun error(error: Error) {
        out.error("e: [$component] ${getMessage(error)}")
        _errorCount++
        if (maxErrors > 0 && _errorCount >= maxErrors) {
            throw ErrorListenerException(Pipeline.Exception("Reached maximum number of errors, aborting execution."))
        }
    }

    override fun warning(error: Error) {
        if (allWarningsAsErrors || _warningsAsErrors.contains(error.code)) {
            error(error)
            return
        }
        if (!inhibitWarnings) {
            out.warn("w: [$component] ${getMessage(error)}")
        }
    }

    fun clear() {
        _errorCount = 0
    }

    fun hasErrors() = _errorCount > 0

    private fun getMessage(error: Error): String {
        val content = when (error.code) {
            ErrorCode.UNEXPECTED_TOKEN -> {
                val tokenName = error.getProperty(Property.TOKEN_NAME)
                val token = error.getProperty(Property.TOKEN_CONTENT)
                "Unexpected token ($tokenName) \"$token\"."
            }
            ErrorCode.UNRECOGNIZED_TOKEN -> {
                val token = error.getProperty(Property.TOKEN_CONTENT)
                "Unrecognized token ($token)."
            }
            ErrorCode.UNDEFINED_FUNCTION -> {
                val functionName = error.getProperty(Property.IDENTIFIER_CHAIN) as Identifier?
                val variants = error.getProperty(Property.FN_VARIANTS) as List<*>?
                val args = error.getProperty(Property.INPUT_ARGUMENT_TYPES) as List<*>?
                buildString {
                    append("Undefined function: ")
                    append(functionName)
                    append(args?.joinToString(", ", "(", ")") { it.toString() })
                    append(".")
                    if (variants != null && variants.isNotEmpty()) {
                        appendLine(" Did you mean: ")
                        for (variant in variants) {
                            variant as Function
                            append("- ")
                            append(variant.getName())
                            append(
                                variant.getParameters().joinToString(", ", "(", ")") {
                                    "${it.getName()}: ${it.getType()}"
                                }
                            )
                            appendLine()
                        }
                    }
                }
            }
            ErrorCode.UNDEFINED_CAST -> {
                val castFrom = error.getProperty(Property.INPUT_TYPE)
                val castTo = error.getProperty(Property.TARGET_TYPE)
                "Undefined cast from $castFrom to $castTo."
            }
            ErrorCode.INTERNAL_ERROR -> {
                when (_verbose) {
                    true -> {
                        val cause = error.getProperty(Property.CAUSE)
                        "Unexpected failure encountered. Caused by: $cause."
                    }
                    false -> "Unexpected failure encountered. Run with --verbose for more information."
                }
            }
            ErrorCode.ALWAYS_MISSING -> {
                "Expression always returns missing."
            }
            else -> "Unrecognized error code received."
        }
        return buildString {
            val line = error.getProperty(Property.LINE_NO) as Int?
            val column = error.getProperty(Property.COLUMN_NO) as Int?
            val length = error.getProperty(Property.LENGTH) as Int?
            val location = getLocationString(line, column, length)
            append(location)
            append(content)
        }
    }

    companion object {

        private fun getLocationString(line: Int?, column: Int?, length: Int?): String {
            return when {
                line == null || column == null -> ""
                length == null -> "$line:$column "
                else -> "$line:$column:$length "
            }
        }
    }
}
