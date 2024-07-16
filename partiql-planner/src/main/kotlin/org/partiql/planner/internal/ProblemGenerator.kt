package org.partiql.planner.internal

import org.partiql.errors.Problem
import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemLocation
import org.partiql.errors.ProblemSeverity
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.plan.Identifier
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpErr
import org.partiql.planner.internal.ir.rexOpMissing
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.types.PType
import org.partiql.types.StaticType
import org.partiql.planner.catalog.Identifier as InternalIdentifier

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

    fun missingRex(causes: List<Rex.Op>, problem: Problem, type: CompilerType = CompilerType(PType.typeDynamic(), isMissingValue = true)): Rex =
        rex(type, rexOpMissing(problem, causes))

    fun missingRex(causes: Rex.Op, problem: Problem, type: CompilerType = CompilerType(PType.typeDynamic(), isMissingValue = true)): Rex =
        rex(type, rexOpMissing(problem, listOf(causes)))

    fun errorRex(causes: List<Rex.Op>, problem: Problem): Rex =
        rex(CompilerType(PType.typeDynamic(), isMissingValue = true), rexOpErr(problem, causes))

    fun errorRex(trace: Rex.Op, problem: Problem): Rex =
        rex(CompilerType(PType.typeDynamic(), isMissingValue = true), rexOpErr(problem, listOf(trace)))

    fun undefinedFunction(identifier: InternalIdentifier, args: List<StaticType>, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UnknownFunction(identifier.toString(), args.map { PType.fromStaticType(it) }))

    fun undefinedFunction(
        args: List<PType>,
        identifier: InternalIdentifier,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION
    ): Problem =
        problem(location, PlanningProblemDetails.UnknownFunction(identifier.toString(), args))

    fun undefinedFunction(identifier: String, args: List<StaticType>, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UnknownFunction(identifier, args.map { PType.fromStaticType(it) }))

    fun undefinedFunction(args: List<PType>, identifier: String, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UnknownFunction(identifier, args))

    fun undefinedVariable(id: Identifier, inScopeVariables: Set<String> = emptySet(), location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UndefinedVariable(id, inScopeVariables))

    fun incompatibleTypesForOp(actualTypes: List<StaticType>, operator: String, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.IncompatibleTypesForOp(actualTypes.map { PType.fromStaticType(it) }, operator))

    fun incompatibleTypesForOp(
        operator: String,
        actualTypes: List<PType>,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION
    ): Problem =
        problem(location, PlanningProblemDetails.IncompatibleTypesForOp(actualTypes, operator))

    fun unresolvedExcludedExprRoot(root: InternalIdentifier, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UnresolvedExcludeExprRoot(root.toString()))

    fun unresolvedExcludedExprRoot(root: String, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UnresolvedExcludeExprRoot(root))

    fun expressionAlwaysReturnsMissing(reason: String? = null, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.ExpressionAlwaysReturnsMissing(reason))

    fun unexpectedType(actualType: StaticType, expectedTypes: Set<StaticType>, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UnexpectedType(PType.fromStaticType(actualType), expectedTypes.map { PType.fromStaticType(it) }.toSet()))

    fun unexpectedType(actualType: PType, expectedTypes: Set<PType>, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.UnexpectedType(actualType, expectedTypes))

    fun compilerError(message: String, location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION): Problem =
        problem(location, PlanningProblemDetails.CompileError(message))
}
