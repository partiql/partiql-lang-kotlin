package org.partiql.planner.util

import org.partiql.errors.Problem
import org.partiql.spi.errors.Error
import org.partiql.spi.errors.ErrorListener

/**
 * An [ErrorListener] that collects all the encountered [Error]s without throwing.
 *
 * This is intended to be used when wanting to collect multiple problems that may be encountered (e.g. a static type
 * inference pass that can result in multiple errors and/or warnings). This handler does not collect other exceptions
 * that may be thrown.
 */
open class ErrorCollector : ErrorListener {
    private val problemList = mutableListOf<Problem>()
    private val errorList = mutableListOf<Error>()
    private val warningList = mutableListOf<Error>()

    val problems: List<Error>
        get() = errorList + warningList

    val errors: List<Error>
        get() = errorList

    val warnings: List<Error>
        get() = warningList

    val hasErrors: Boolean
        get() = errorList.isNotEmpty()

    val hasWarnings: Boolean
        get() = warningList.isNotEmpty()

    override fun error(error: Error) {
        errorList.add(error)
    }

    override fun warning(error: Error) {
        warningList.add(error)
    }
}
