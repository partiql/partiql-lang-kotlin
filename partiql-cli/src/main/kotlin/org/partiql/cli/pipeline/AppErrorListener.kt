package org.partiql.cli.pipeline

import org.partiql.cli.ErrorCodeString
import org.partiql.cli.shell.error
import org.partiql.cli.shell.warn
import org.partiql.spi.errors.Error
import org.partiql.spi.errors.ErrorListener
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
        val message = ErrorMessageFormatter.message(error)
        out.error("e: [$component] $message")
        _errorCount++
        if (maxErrors in 1.._errorCount) {
            throw Pipeline.PipelineException("Reached maximum number of errors, aborting execution.")
        }
    }

    override fun warning(error: Error) {
        if (allWarningsAsErrors || _warningsAsErrors.contains(error.code)) {
            error(error)
            return
        }
        if (!inhibitWarnings) {
            val message = ErrorMessageFormatter.message(error)
            out.warn("w: [$component] $message")
        }
    }

    fun clear() {
        _errorCount = 0
    }

    fun hasErrors() = _errorCount > 0
}
