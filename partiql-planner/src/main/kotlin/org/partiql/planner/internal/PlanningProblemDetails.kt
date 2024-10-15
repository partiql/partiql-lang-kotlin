package org.partiql.planner.internal

import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemSeverity
import org.partiql.planner.internal.problems.AlwaysMissing
import org.partiql.planner.internal.problems.CastUndefined
import org.partiql.planner.internal.problems.FunctionTypeMismatch
import org.partiql.planner.internal.problems.TypeUnexpected
import org.partiql.planner.internal.problems.VarRefNotFound
import org.partiql.spi.SourceLocation
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.Classification
import org.partiql.spi.errors.PError
import org.partiql.types.PType

/**
 * Contains detailed information about errors that may occur during query planning.
 *
 * This information can be used to generate end-user readable error messages and is also easy to assert
 * equivalence in unit tests.
 */
internal open class PlanningProblemDetails(
    override val severity: ProblemSeverity,
    val messageFormatter: () -> String,
) : ProblemDetails {

    /**
     * TODO: This is a temporary internal measure to convert the old PlanningProblemDetails to
     *  the new error reporting mechanism.
     */
    open fun toError(line: Int?, column: Int?, length: Int?): PError {
        val location = location(line, column, length)
        return PError.INTERNAL_ERROR(Classification.SEMANTIC(), location, null)
    }

    companion object {
        private fun quotationHint(caseSensitive: Boolean) =
            if (caseSensitive) {
                // Individuals that are new to SQL often try to use double quotes for string literals.
                // Let's help them out a bit.
                " Hint: did you intend to use single-quotes (') here?  Remember that double-quotes (\") denote " +
                    "quoted identifiers and single-quotes denote strings."
            } else {
                ""
            }
    }

    override fun toString(): String = message
    override val message: String get() = messageFormatter()

    public data class UndefinedVariable(
        val name: Identifier,
        val inScopeVariables: Set<String>
    ) : PlanningProblemDetails(
        ProblemSeverity.ERROR,
        {
            "Variable $name does not exist in the database environment and is not an attribute of the following in-scope variables $inScopeVariables." +
                quotationHint(isSymbolAndCaseSensitive(name))
        }
    ) {

        override fun toError(line: Int?, column: Int?, length: Int?): PError {
            val location = location(line, column, length)
            return VarRefNotFound(location, name, inScopeVariables.toList())
        }

        private companion object {

            private fun isSymbolAndCaseSensitive(id: Identifier): Boolean {
                if (id.hasQualifier()) {
                    return false
                }
                return !id.getIdentifier().isRegular()
            }
        }
    }

    data class UnexpectedType(
        val actualType: PType,
        val expectedTypes: Set<PType>,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        "Unexpected type $actualType, expected one of ${expectedTypes.joinToString { it.toString() }}"
    }) {
        override fun toError(line: Int?, column: Int?, length: Int?): PError {
            val location = location(line, column, length)
            return TypeUnexpected(location, actualType, expectedTypes.toList())
        }
    }

    data class UnknownCast(
        val source: PType,
        val target: PType,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        "Cast does not exist for $source to $target."
    }) {
        override fun toError(line: Int?, column: Int?, length: Int?): PError {
            val location = location(line, column, length)
            return CastUndefined(location, source, target)
        }
    }

    data class ExpressionAlwaysReturnsMissing(val reason: String? = null) : PlanningProblemDetails(
        severity = ProblemSeverity.ERROR,
        messageFormatter = { "Expression always returns missing: caused by $reason" }
    ) {
        override fun toError(line: Int?, column: Int?, length: Int?): PError {
            val location = location(line, column, length)
            return AlwaysMissing(location)
        }
    }

    data class IncompatibleTypesForOp(
        val actualTypes: List<PType>,
        val operator: String,
    ) : PlanningProblemDetails(
        severity = ProblemSeverity.ERROR,
        messageFormatter = { "${actualTypes.joinToString { it.toString() }} is/are incompatible data types for the '$operator' operator." }
    ) {
        override fun toError(line: Int?, column: Int?, length: Int?): PError {
            val location = location(line, column, length)
            return FunctionTypeMismatch(location, Identifier.delimited(operator), actualTypes, null)
        }
    }

    protected fun location(line: Int?, column: Int?, length: Int?): SourceLocation? {
        return when (line != null && column != null && length != null) {
            true -> SourceLocation(line.toLong(), column.toLong(), length.toLong())
            false -> null
        }
    }
}
