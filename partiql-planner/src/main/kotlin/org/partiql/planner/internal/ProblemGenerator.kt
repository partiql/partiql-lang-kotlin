package org.partiql.planner.internal

import org.partiql.errors.Problem
import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemLocation
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpErr
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.Error
import org.partiql.spi.errors.ErrorCode
import org.partiql.spi.errors.ErrorListener
import org.partiql.spi.function.Function
import org.partiql.types.PType
import org.partiql.types.StaticType

/**
 * Used to report problems during planning phase.
 */
internal object ProblemGenerator {
    fun problem(problemLocation: ProblemLocation, problemDetails: ProblemDetails): Problem = Problem(
        problemLocation, problemDetails
    )

    private fun errorRex(
        type: CompilerType = CompilerType(PType.dynamic(), isMissingValue = true),
    ): Rex {
        return rex(CompilerType(PType.dynamic(), isMissingValue = true), rexOpErr())
    }

    private fun undefinedFunction(
        args: List<PType>,
        identifier: Identifier,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
        variants: List<Function> = emptyList(),
    ): Problem {
        return problem(location, PlanningProblemDetails.UnknownFunction(identifier, args, variants))
    }

    fun reportUndefinedFunction(
        listener: ErrorListener,
        args: List<PType>,
        identifier: Identifier,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
        variants: List<Function> = emptyList()
    ): Rex {
        val problem = undefinedFunction(args, identifier, location, variants)
        listener.error(problem.toError())
        return errorRex()
    }

    private fun undefinedCast(
        source: PType,
        target: PType,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION
    ): Problem {
        return problem(location, PlanningProblemDetails.UnknownCast(source, target))
    }

    fun reportUndefinedCast(
        listener: ErrorListener,
        source: PType,
        target: PType,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION
    ): Rex {
        val problem = undefinedCast(source, target, location)
        listener.warning(problem.toError()) // TODO: Should this really be a warning?
        return errorRex()
    }

    // TODO: Make this private
    fun undefinedVariable(
        id: Identifier,
        inScopeVariables: Set<String> = emptySet(),
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Problem {
        return problem(location, PlanningProblemDetails.UndefinedVariable(id, inScopeVariables))
    }

    /**
     * Emits an error to the [listener].
     */
    fun reportUndefinedVariable(
        listener: ErrorListener,
        id: Identifier,
        inScopeVariables: Set<String> = emptySet(),
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Rex {
        val problem = undefinedVariable(id, inScopeVariables, location)
        listener.error(problem.toError())
        return errorRex()
    }

    fun incompatibleTypesForOp(
        actualTypes: List<StaticType>,
        operator: String,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Problem {
        return problem(
            location,
            PlanningProblemDetails.IncompatibleTypesForOp(
                actualTypes.map { PType.fromStaticType(it) }, operator.uppercase()
            )
        )
    }

    // TODO: Make private
    fun alwaysMissing(
        reason: String? = null,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Problem {
        return problem(location, PlanningProblemDetails.ExpressionAlwaysReturnsMissing(reason))
    }

    fun reportAlwaysMissing(
        listener: ErrorListener,
        reason: String? = null, // TODO: Is the reason needed?
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Rex {
        val problem = alwaysMissing(reason, location)
        listener.warning(problem.toError())
        return errorRex()
    }

    // TODO: Make private
    fun unexpectedType(
        actualType: StaticType,
        expectedTypes: Set<StaticType>,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Problem = problem(
        location,
        PlanningProblemDetails.UnexpectedType(
            PType.fromStaticType(actualType), expectedTypes.map { PType.fromStaticType(it) }.toSet()
        )
    )

    private fun unexpectedType(
        actualType: PType,
        expectedTypes: Set<PType>,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Problem {
        return problem(location, PlanningProblemDetails.UnexpectedType(actualType, expectedTypes))
    }

    fun reportUnexpectedType(
        listener: ErrorListener,
        actualType: PType,
        expectedTypes: Set<PType>,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Rex {
        val problem = unexpectedType(actualType, expectedTypes, location)
        listener.warning(problem.toError()) // TODO: Is this really a warning?
        return errorRex()
    }

    private fun Problem.toError(): Error {
        val location = this.sourceLocation
        val line = if (location.lineNum < 0) null else location.lineNum
        val column = if (location.charOffset < 0) null else location.charOffset
        val length = if (location.length < 0) null else location.length
        return when (val details = this.details) {
            is PlanningProblemDetails -> details.toError(line?.toInt(), column?.toInt(), length?.toInt())
            else -> Error.of(ErrorCode.INTERNAL_ERROR)
        }
    }
}
    