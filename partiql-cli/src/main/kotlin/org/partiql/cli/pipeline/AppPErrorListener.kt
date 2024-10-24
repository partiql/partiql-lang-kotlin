package org.partiql.cli.pipeline

import org.partiql.cli.ErrorCodeString
import org.partiql.cli.shell.error
import org.partiql.cli.shell.warn
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.errors.Severity
import java.io.PrintStream

class AppPErrorListener(
    private val out: PrintStream,
    private val maxErrors: Int,
    private val inhibitWarnings: Boolean,
    warningsAsErrors: Array<ErrorCodeString>
) : PErrorListener {

    private val _warningsAsErrors = warningsAsErrors.map { it.code }.toSet()
    private var _errorCount: Int = 0
    private val allWarningsAsErrors: Boolean = _warningsAsErrors.contains(ErrorCodeString.ALL.code)

    override fun report(error: PError) {
        when (error.severity.code()) {
            Severity.ERROR -> error(error)
            Severity.WARNING -> warning(error)
            else -> error("This shouldn't have occurred.")
        }
    }

    private fun error(error: PError) {
        val message = ErrorMessageFormatter.message(error)
        out.error(message)
        _errorCount++
        if (maxErrors in 1.._errorCount) {
            throw Pipeline.PipelineException("Reached maximum number of errors, aborting execution.")
        }
        if (error.kind.code() == PErrorKind.SYNTAX) {
            throw Pipeline.PipelineException("Unexpected syntax error. Please see the above details.")
        }
    }

    private fun warning(error: PError) {
        if (allWarningsAsErrors || _warningsAsErrors.contains(error.code())) {
            error(error)
            return
        }
        if (!inhibitWarnings) {
            val message = ErrorMessageFormatter.message(error)
            out.warn(message)
        }
    }

    fun clear() {
        _errorCount = 0
    }

    fun hasErrors() = _errorCount > 0
}
