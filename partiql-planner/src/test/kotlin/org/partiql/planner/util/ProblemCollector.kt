package org.partiql.planner.util

import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.errors.ProblemHandler
import org.partiql.errors.ProblemSeverity

/**
 * A [ProblemHandler] that collects all the encountered [Problem]s without throwing.
 *
 * This is intended to be used when wanting to collect multiple problems that may be encountered (e.g. a static type
 * inference pass that can result in multiple errors and/or warnings). This handler does not collect other exceptions
 * that may be thrown.
 */
internal class ProblemCollector : ProblemCallback {
    private val problemList = mutableListOf<Problem>()

    val problems: List<Problem>
        get() = problemList

    val hasErrors: Boolean
        get() = problemList.any { it.details.severity == ProblemSeverity.ERROR }

    val hasWarnings: Boolean
        get() = problemList.any { it.details.severity == ProblemSeverity.WARNING }

    override fun invoke(problem: Problem) {
        problemList.add(problem)
    }
}
