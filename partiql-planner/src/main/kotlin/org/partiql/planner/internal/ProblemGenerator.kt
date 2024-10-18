package org.partiql.planner.internal

import org.partiql.errors.Problem
import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemLocation
import org.partiql.errors.ProblemSeverity
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.spi.SourceLocation
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.types.PType
import org.partiql.types.StaticType

/**
 * Used to report problems during planning phase.
 * TODO: Delete this. This is currently only used by the test source.
 */
internal object ProblemGenerator {
    private fun problem(problemLocation: ProblemLocation, problemDetails: ProblemDetails): Problem = Problem(
        problemLocation, problemDetails
    )

    // TODO: Make this private
    fun undefinedVariable(
        id: Identifier,
        inScopeVariables: Set<String> = emptySet(),
        location: ProblemLocation = UNKNOWN_PROBLEM_LOCATION,
    ): Problem {
        return problem(location, PlanningProblemDetails.UndefinedVariable(id, inScopeVariables))
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
                    return PErrors.pathIndexNeverSucceeds(location(location))
                }
            }
        )
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

    internal fun Problem.toError(): PError {
        val location = this.sourceLocation
        val line = if (location.lineNum < 0) null else location.lineNum
        val column = if (location.charOffset < 0) null else location.charOffset
        val length = if (location.length < 0) null else location.length
        return when (val details = this.details) {
            is PlanningProblemDetails -> details.toError(line?.toInt(), column?.toInt(), length?.toInt())
            else -> PError.INTERNAL_ERROR(PErrorKind.SEMANTIC(), location(location), null)
        }
    }

    private fun location(loc: ProblemLocation): SourceLocation? {
        return when (loc.lineNum < 0 || loc.charOffset < 0 || loc.length < 0) {
            true -> null
            false -> SourceLocation(loc.lineNum, loc.charOffset, loc.length)
        }
    }
}
