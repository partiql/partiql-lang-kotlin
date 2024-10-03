package org.partiql.eval.internal

import org.partiql.errors.Problem
import org.partiql.errors.ProblemHandler

/**
 * A [ProblemHandler] that executes a sequence of [ProblemHandler]s.
 *
 * @param handlers the sequence of [ProblemHandler]s to execute
 *
 * @see ProblemHandler
 * @see Problem
 */
internal class ProblemHandlerSequence(
    handlers: List<ProblemHandler>
) : ProblemHandler {

    private val _handlers = handlers

    override fun handleProblem(problem: Problem) {
        _handlers.forEach {
            it.handleProblem(problem)
        }
    }
}
