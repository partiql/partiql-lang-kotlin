package org.partiql.planner.internal

import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemSeverity
import org.partiql.spi.catalog.Identifier
import org.partiql.types.PType
import org.partiql.types.StaticType

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

    data class ParseError(val parseErrorMessage: String) :
        PlanningProblemDetails(ProblemSeverity.ERROR, { parseErrorMessage })

    data class CompileError(val errorMessage: String) :
        PlanningProblemDetails(ProblemSeverity.ERROR, { errorMessage })

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

        private companion object {

            private fun isSymbolAndCaseSensitive(id: Identifier): Boolean {
                if (id.hasQualifier()) {
                    return false
                }
                return !id.getIdentifier().isRegular()
            }
        }
    }

    public data class UndefinedDmlTarget(val variableName: String, val caseSensitive: Boolean) :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            {
                "Data manipulation target table '$variableName' is undefined. " +
                    "Hint: this must be a name in the global scope. " +
                    quotationHint(caseSensitive)
            }
        )

    data class VariablePreviouslyDefined(val variableName: String) :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            { "The variable '$variableName' was previously defined." }
        )

    data class UnimplementedFeature(val featureName: String) :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            { "The syntax at this location is valid but utilizes unimplemented PartiQL feature '$featureName'" }
        )

    object InvalidDmlTarget :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            { "Expression is not a valid DML target.  Hint: only table names are allowed here." }
        )

    object InsertValueDisallowed :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            {
                "Use of `INSERT INTO <table> VALUE <expr>` is not allowed. " +
                    "Please use the `INSERT INTO <table> << <expr> >>` form instead."
            }
        )

    object InsertValuesDisallowed :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            {
                "Use of `VALUES (<expr>, ...)` with INSERT is not allowed. " +
                    "Please use the `INSERT INTO <table> << <expr>, ... >>` form instead."
            }
        )

    data class UnexpectedType(
        val actualType: PType,
        val expectedTypes: Set<PType>,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        "Unexpected type $actualType, expected one of ${expectedTypes.joinToString { it.toString() }}"
    })

    data class UnknownFunction(
        val identifier: String,
        val args: List<PType>,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        val types = args.joinToString { "<${it.toString().lowercase()}>" }
        "Unknown function `$identifier($types)"
    })

    data class UnknownCast(
        val source: PType,
        val target: PType,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        "Cast does not exist for $source to $target."
    })

    public data class UnknownAggregateFunction(
        val identifier: Identifier,
        val args: List<StaticType>,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        val types = args.joinToString { "<${it.toString().lowercase()}>" }
        "Unknown aggregate function `$identifier($types)"
    })

    public object ExpressionAlwaysReturnsNullOrMissing : PlanningProblemDetails(
        severity = ProblemSeverity.ERROR,
        messageFormatter = { "Expression always returns null or missing." }
    )

    data class ExpressionAlwaysReturnsMissing(val reason: String? = null) : PlanningProblemDetails(
        severity = ProblemSeverity.ERROR,
        messageFormatter = { "Expression always returns missing: caused by $reason" }
    )

    data class InvalidArgumentTypeForFunction(
        val functionName: String,
        val expectedType: StaticType,
        val actualType: StaticType,
    ) :
        PlanningProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "Invalid argument type for $functionName. Expected $expectedType but got $actualType" }
        )

    data class IncompatibleTypesForOp(
        val actualTypes: List<PType>,
        val operator: String,
    ) :
        PlanningProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "${actualTypes.joinToString { it.toString() }} is/are incompatible data types for the '$operator' operator." }
        )

    data class UnresolvedExcludeExprRoot(val root: String) :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            { "Exclude expression given an unresolvable root '$root'" }
        )
}
