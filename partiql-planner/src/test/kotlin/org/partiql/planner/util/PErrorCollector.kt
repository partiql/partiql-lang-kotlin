package org.partiql.planner.util

import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.errors.Severity

/**
 * An [PErrorListener] that collects all the encountered [PError]s without throwing.
 *
 * This is intended to be used when wanting to collect multiple problems that may be encountered (e.g. a static type
 * inference pass that can result in multiple errors and/or warnings). This handler does not collect other exceptions
 * that may be thrown.
 */
open class PErrorCollector : PErrorListener {
    private val errorList = mutableListOf<PError>()
    private val warningList = mutableListOf<PError>()

    val problems: List<PError>
        get() = errorList + warningList

    val errors: List<PError>
        get() = errorList

    val warnings: List<PError>
        get() = warningList

    override fun report(error: PError) {
        when (error.severity.code()) {
            Severity.ERROR -> errorList.add(error)
            Severity.WARNING -> warningList.add(error)
            else -> error("Unsupported severity.")
        }
    }
}
