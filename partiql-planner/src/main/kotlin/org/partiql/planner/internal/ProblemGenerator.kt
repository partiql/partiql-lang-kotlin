package org.partiql.planner.internal

import org.partiql.errors.Problem
import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemLocation
import org.partiql.errors.ProblemSeverity
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpErr
import org.partiql.planner.internal.typer.CompilerType
import org.partiql.spi.SourceLocation
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.Error
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

    fun reportUndefinedFunction(
        listener: ErrorListener,
        args: List<PType>,
        identifier: Identifier,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
        variants: List<Function> = emptyList()
    ): Rex {
        val loc = location(location)
        when (variants.isEmpty()) {
            true -> listener.error(Error.FUNCTION_NOT_FOUND(loc, identifier, args))
            false -> listener.warning(Error.FUNCTION_TYPE_MISMATCH(loc, identifier, args, variants))
        }
        return errorRex()
    }

    fun reportFunctionMistyped(
        listener: ErrorListener,
        args: List<PType>,
        identifier: Identifier,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
        variants: List<Function> = emptyList()
    ): Rex {
        val loc = location(location)
        listener.warning(Error.FUNCTION_TYPE_MISMATCH(loc, identifier, args, variants))
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
        listener.warning(problem.toError())
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
                override fun toError(line: Int?, column: Int?, length: Int?): Error {
                    return Error.PATH_INDEX_NEVER_SUCCEEDS(location(location))
                }
            }
        )
    }

    fun reportAlwaysMissing(
        listener: ErrorListener,
        code: Int,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Rex {
        val loc = location(location)
        val error = when (code) {
            Error.PATH_KEY_NEVER_SUCCEEDS -> Error.PATH_KEY_NEVER_SUCCEEDS(loc)
            Error.PATH_SYMBOL_NEVER_SUCCEEDS -> Error.PATH_SYMBOL_NEVER_SUCCEEDS(loc)
            Error.PATH_INDEX_NEVER_SUCCEEDS -> Error.PATH_INDEX_NEVER_SUCCEEDS(loc)
            Error.ALWAYS_MISSING -> Error.ALWAYS_MISSING(loc)
            else -> error("This is an internal bug. This shouldn't have occurred.")
        }
        listener.warning(error)
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
        listener: ErrorListener,
        actualType: PType,
        expectedTypes: Set<PType>,
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Rex {
        val problem = unexpectedType(actualType, expectedTypes, location)
        listener.warning(problem.toError()) // TODO: Is this really a warning?
        return errorRex()
    }

    internal fun Problem.toError(): Error {
        val location = this.sourceLocation
        val line = if (location.lineNum < 0) null else location.lineNum
        val column = if (location.charOffset < 0) null else location.charOffset
        val length = if (location.length < 0) null else location.length
        return when (val details = this.details) {
            is PlanningProblemDetails -> details.toError(line?.toInt(), column?.toInt(), length?.toInt())
            else -> Error.INTERNAL_ERROR(location(location), null)
        }
    }

    private fun location(loc: ProblemLocation): SourceLocation? {
        return when (loc.lineNum < 0 || loc.charOffset < 0 || loc.length < 0) {
            true -> null
            false -> SourceLocation(loc.lineNum, loc.charOffset, loc.length)
        }
    }
}
