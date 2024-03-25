package org.partiql.planner

import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemSeverity
import org.partiql.plan.Identifier
import org.partiql.types.StaticType

/**
 * Contains detailed information about errors that may occur during query planning.
 *
 * This information can be used to generate end-user readable error messages and is also easy to assert
 * equivalence in unit tests.
 */
public sealed class PlanningProblemDetails(
    override val severity: ProblemSeverity,
    public val messageFormatter: () -> String,
) : ProblemDetails {

    override fun toString(): String = message
    override val message: String get() = messageFormatter()

    public data class ParseError(val parseErrorMessage: String) :
        PlanningProblemDetails(ProblemSeverity.ERROR, { parseErrorMessage })

    public data class CompileError(val errorMessage: String) :
        PlanningProblemDetails(ProblemSeverity.ERROR, { errorMessage })

    public data class UndefinedVariable(
        val name: Identifier,
        val inScopeVariables: Set<String>
    ) : PlanningProblemDetails(
        ProblemSeverity.ERROR,
        {
            "Variable ${pretty(name)} does not exist in the database environment and is not an attribute of the following in-scope variables $inScopeVariables." +
                quotationHint(isSymbolAndCaseSensitive(name))
        }
    ) {

        @Deprecated("This will be removed in a future major version release.", replaceWith = ReplaceWith("name"))
        val variableName: String = when (name) {
            is Identifier.Symbol -> name.symbol
            is Identifier.Qualified -> when (name.steps.size) {
                0 -> name.root.symbol
                else -> name.steps.last().symbol
            }
        }

        @Deprecated("This will be removed in a future major version release.", replaceWith = ReplaceWith("name"))
        val caseSensitive: Boolean = when (name) {
            is Identifier.Symbol -> name.caseSensitivity == Identifier.CaseSensitivity.SENSITIVE
            is Identifier.Qualified -> when (name.steps.size) {
                0 -> name.root.caseSensitivity == Identifier.CaseSensitivity.SENSITIVE
                else -> name.steps.last().caseSensitivity == Identifier.CaseSensitivity.SENSITIVE
            }
        }

        @Deprecated("This will be removed in a future major version release.", replaceWith = ReplaceWith("UndefinedVariable(Identifier, Set<String>)"))
        public constructor(variableName: String, caseSensitive: Boolean) : this(
            Identifier.Symbol(
                variableName,
                when (caseSensitive) {
                    true -> Identifier.CaseSensitivity.SENSITIVE
                    false -> Identifier.CaseSensitivity.INSENSITIVE
                }
            ),
            emptySet()
        )

        private companion object {
            /**
             * Used to check whether the [id] is an [Identifier.Symbol] and whether it is case-sensitive. This is helpful
             * for giving the [quotationHint] to the user.
             */
            private fun isSymbolAndCaseSensitive(id: Identifier): Boolean = when (id) {
                is Identifier.Symbol -> id.caseSensitivity == Identifier.CaseSensitivity.SENSITIVE
                is Identifier.Qualified -> false
            }

            private fun pretty(id: Identifier): String = when (id) {
                is Identifier.Symbol -> pretty(id)
                is Identifier.Qualified -> (listOf(id.root) + id.steps).joinToString(".") { pretty(it) }
            }

            private fun pretty(id: Identifier.Symbol): String = when (id.caseSensitivity) {
                Identifier.CaseSensitivity.INSENSITIVE -> id.symbol
                Identifier.CaseSensitivity.SENSITIVE -> "\"${id.symbol}\""
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

    public data class VariablePreviouslyDefined(val variableName: String) :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            { "The variable '$variableName' was previously defined." }
        )

    public data class UnimplementedFeature(val featureName: String) :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            { "The syntax at this location is valid but utilizes unimplemented PartiQL feature '$featureName'" }
        )

    public object InvalidDmlTarget :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            { "Expression is not a valid DML target.  Hint: only table names are allowed here." }
        )

    public object InsertValueDisallowed :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            {
                "Use of `INSERT INTO <table> VALUE <expr>` is not allowed. " +
                    "Please use the `INSERT INTO <table> << <expr> >>` form instead."
            }
        )

    public object InsertValuesDisallowed :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            {
                "Use of `VALUES (<expr>, ...)` with INSERT is not allowed. " +
                    "Please use the `INSERT INTO <table> << <expr>, ... >>` form instead."
            }
        )

    public data class UnexpectedType(
        val actualType: StaticType,
        val expectedTypes: Set<StaticType>,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        "Unexpected type $actualType, expected one of ${expectedTypes.joinToString()}"
    })

    public data class UnknownFunction(
        val identifier: String,
        val args: List<StaticType>,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        val types = args.joinToString { "<${it.toString().lowercase()}>" }
        "Unknown function `$identifier($types)"
    })

    public data class UnknownAggregateFunction(
        val identifier: String,
        val args: List<StaticType>,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        val types = args.joinToString { "<${it.toString().lowercase()}>" }
        "Unknown aggregate function `$identifier($types)"
    })

    public object ExpressionAlwaysReturnsNullOrMissing : PlanningProblemDetails(
        severity = ProblemSeverity.ERROR,
        messageFormatter = { "Expression always returns null or missing." }
    )

    public data class InvalidArgumentTypeForFunction(
        val functionName: String,
        val expectedType: StaticType,
        val actualType: StaticType,
    ) :
        PlanningProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "Invalid argument type for $functionName. Expected $expectedType but got $actualType" }
        )

    public data class IncompatibleTypesForOp(
        val actualTypes: List<StaticType>,
        val operator: String,
    ) :
        PlanningProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "${actualTypes.joinToString()} is/are incompatible data types for the '$operator' operator." }
        )

    public data class UnresolvedExcludeExprRoot(val root: String) :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            { "Exclude expression given an unresolvable root '$root'" }
        )
}

private fun quotationHint(caseSensitive: Boolean) =
    if (caseSensitive) {
        // Individuals that are new to SQL often try to use double quotes for string literals.
        // Let's help them out a bit.
        " Hint: did you intend to use single-quotes (') here?  Remember that double-quotes (\") denote " +
            "quoted identifiers and single-quotes denote strings."
    } else {
        ""
    }
