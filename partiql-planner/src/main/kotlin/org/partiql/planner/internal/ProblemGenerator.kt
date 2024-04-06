package org.partiql.planner.internal

import org.partiql.errors.Problem
import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemLocation
import org.partiql.errors.ProblemSeverity
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpErr
import org.partiql.planner.internal.ir.rexOpMissing
import org.partiql.spi.BindingPath
import org.partiql.types.StaticType
import org.partiql.planner.internal.ir.Identifier as InternalIdentifier

/**
 * Used to report problems during planning phase.
 */
internal object ProblemGenerator {
    fun problem(problemLocation: ProblemLocation, problemDetails: ProblemDetails): Problem = Problem(
        problemLocation,
        problemDetails
    )

    fun asWarning(problem: Problem): Problem {
        val details = problem.details as PlanningProblemDetails
        return if (details.severity == ProblemSeverity.WARNING) problem
        else Problem(
            problem.sourceLocation,
            PlanningProblemDetails(ProblemSeverity.WARNING, details.messageFormatter)
        )
    }
    fun asError(problem: Problem): Problem {
        val details = problem.details as PlanningProblemDetails
        return if (details.severity == ProblemSeverity.ERROR) problem
        else Problem(
            problem.sourceLocation,
            PlanningProblemDetails(ProblemSeverity.ERROR, details.messageFormatter)
        )
    }

    fun missingRex(causes: List<Rex.Op>, problem: Problem): Rex =
        rex(StaticType.MISSING, rexOpMissing(problem, causes))

    fun missingRex(causes: Rex.Op, problem: Problem): Rex =
        rex(StaticType.MISSING, rexOpMissing(problem, listOf(causes)))

    fun errorRex(causes: List<Rex.Op>, problem: Problem): Rex =
        rex(StaticType.ANY, rexOpErr(problem, causes))

    fun errorRex(trace: Rex.Op, problem: Problem): Rex =
        rex(StaticType.ANY, rexOpErr(problem, listOf(trace)))

    private fun InternalIdentifier.debug(): String = when (this) {
        is InternalIdentifier.Qualified -> (listOf(root.debug()) + steps.map { it.debug() }).joinToString(".")
        is InternalIdentifier.Symbol -> when (caseSensitivity) {
            InternalIdentifier.CaseSensitivity.SENSITIVE -> "\"$symbol\""
            InternalIdentifier.CaseSensitivity.INSENSITIVE -> symbol
        }
    }

    fun undefinedFunction(identifier: InternalIdentifier, args: List<StaticType>, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UnknownFunction(identifier.debug(), args))

    fun undefinedFunction(identifier: String, args: List<StaticType>, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UnknownFunction(identifier, args))

    fun undefinedVariable(id: BindingPath, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UndefinedVariable(id))

    fun incompatibleTypesForOp(actualTypes: List<StaticType>, operator: String, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.IncompatibleTypesForOp(actualTypes, operator))

    fun unresolvedExcludedExprRoot(root: InternalIdentifier, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UnresolvedExcludeExprRoot(root.debug()))

    fun unresolvedExcludedExprRoot(root: String, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UnresolvedExcludeExprRoot(root))

    fun expressionAlwaysReturnsMissing(reason: String? = null, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.ExpressionAlwaysReturnsMissing(reason))

    fun unexpectedType(actualType: StaticType, expectedTypes: Set<StaticType>, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UnexpectedType(actualType, expectedTypes))

    fun compilerError(message: String, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.CompileError(message))
}
