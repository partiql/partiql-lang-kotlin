package org.partiql.lang.errors

import org.partiql.errors.Problem
import org.partiql.errors.ProblemHandler
import org.partiql.errors.ProblemSeverity
import org.partiql.lang.ast.passes.SemanticException

/**
 * A [ProblemHandler] that collects all of the encountered [Problem]s without throwing.
 *
 * This is intended to be used when wanting to collect multiple problems that may be encountered (e.g. a static type
 * inference pass that can result in multiple errors and/or warnings). This handler does not collect other exceptions
 * that may be thrown.
 */
class ProblemCollector : ProblemHandler {
    private val problemList = mutableListOf<Problem>()

    val problems: List<Problem>
        get() = problemList

    val hasErrors: Boolean
        get() = problemList.any { it.details.severity == ProblemSeverity.ERROR }

    val hasWarnings: Boolean
        get() = problemList.any { it.details.severity == ProblemSeverity.WARNING }

    override fun handleProblem(problem: Problem) {
        problemList.add(problem)
    }
}

/**
 * A [ProblemHandler] that throws the first [Problem] that has a [ProblemSeverity] of [ProblemSeverity.ERROR] as a
 * [SemanticException].
 *
 * This is intended to support existing internal code (e.g. CompilerPipeline, StaticTypeInferenceVisitorTransform)
 * behavior that expects the first encountered problem to be thrown. Once multiple problem handling is supported
 * in that code, this class can be removed.
 *
 * @throws SemanticException on the first [Problem] logged with severity of [ProblemSeverity.ERROR]
 */
internal class ProblemThrower : ProblemHandler {
    override fun handleProblem(problem: Problem) {
        if (problem.details.severity == ProblemSeverity.ERROR) {
            throw SemanticException(problem)
        }
    }
}
