package org.partiql.planner.internal

import org.partiql.errors.Problem
import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemLocation
import org.partiql.errors.ProblemSeverity
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpErr
import org.partiql.planner.internal.problems.AlwaysMissing
import org.partiql.planner.internal.problems.CastUndefined
import org.partiql.planner.internal.problems.FunctionNotFound
import org.partiql.planner.internal.problems.FunctionTypeMismatch
import org.partiql.planner.internal.problems.PathIndexNeverSucceeds
import org.partiql.planner.internal.problems.PathKeyNeverSucceeds
import org.partiql.planner.internal.problems.PathSymbolNeverSucceeds
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.spi.SourceLocation
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.Classification
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorListener
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

    fun reportUndefinedFunction(
        listener: PErrorListener,
        args: List<PType>,
        identifier: Identifier,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
        variants: List<Function> = emptyList()
    ): Rex {
        val loc = location(location)
        when (variants.isEmpty()) {
            true -> listener.report(FunctionNotFound(loc, identifier, args))
            false -> listener.report(FunctionTypeMismatch(loc, identifier, args, variants))
        }
        return errorRex()
    }

    fun reportFunctionMistyped(
        listener: PErrorListener,
        args: List<PType>,
        identifier: Identifier,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
        variants: List<Function> = emptyList()
    ): Rex {
        val loc = location(location)
        listener.report(FunctionTypeMismatch(loc, identifier, args, variants))
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
        listener: PErrorListener,
        source: PType,
        target: PType,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION
    ): Rex {
        val problem = CastUndefined(location(location), source, target)
        listener.report(problem) // TODO: Should this really be a warning?
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
        listener: PErrorListener,
        id: Identifier,
        inScopeVariables: Set<String> = emptySet(),
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Rex {
        val problem = undefinedVariable(id, inScopeVariables, location)
        listener.report(problem.toError())
        return errorRex()
    }

    fun incompatibleTypesForOp(
        actualTypes: List<StaticType>,
        operator: String,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Problem {
        return problem(
            location,
            PlanningProblemDetails.IncompatibleTypesForOp(actualTypes.map { PType.fromStaticType(it) }, operator.uppercase())
        )
    }

    // TODO: Make private
    fun alwaysMissing(
        reason: String? = null,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Problem {
        return problem(location, PlanningProblemDetails.ExpressionAlwaysReturnsMissing(reason))
    }

    fun pathIndexNeverSucceeds(
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Problem {
        return problem(
            location,
            object : PlanningProblemDetails(ProblemSeverity.WARNING, { "" }) {
                override fun toError(line: Int?, column: Int?, length: Int?): PError {
                    return PathIndexNeverSucceeds(location(location))
                }
            }
        )
    }

    fun reportAlwaysMissing(
        listener: PErrorListener,
        code: Int,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Rex {
        val loc = location(location)
        val error = when (code) {
            PError.PATH_KEY_NEVER_SUCCEEDS -> PathKeyNeverSucceeds(loc)
            PError.PATH_SYMBOL_NEVER_SUCCEEDS -> PathSymbolNeverSucceeds(loc)
            PError.PATH_INDEX_NEVER_SUCCEEDS -> PathIndexNeverSucceeds(loc)
            PError.ALWAYS_MISSING -> AlwaysMissing(loc)
            else -> error("This is an internal bug. This shouldn't have occurred.")
        }
        listener.report(error)
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
            PType.fromStaticType(actualType),
            expectedTypes.map { PType.fromStaticType(it) }.toSet()
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
        listener: PErrorListener,
        actualType: PType,
        expectedTypes: Set<PType>,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Rex {
        val problem = unexpectedType(actualType, expectedTypes, location)
        listener.report(problem.toError()) // TODO: Is this really a warning?
        return errorRex()
    }

    internal fun Problem.toError(): PError {
        val location = this.sourceLocation
        val line = if (location.lineNum < 0) null else location.lineNum
        val column = if (location.charOffset < 0) null else location.charOffset
        val length = if (location.length < 0) null else location.length
        return when (val details = this.details) {
            is PlanningProblemDetails -> details.toError(line?.toInt(), column?.toInt(), length?.toInt())
            else -> PError.INTERNAL_ERROR(Classification.SEMANTIC(), location(location), null)
        }
    }

    private fun location(loc: ProblemLocation): SourceLocation? {
        return when (loc.lineNum < 0 || loc.charOffset < 0 || loc.length < 0) {
            true -> null
            false -> SourceLocation(loc.lineNum, loc.charOffset, loc.length)
        }
    }
}
