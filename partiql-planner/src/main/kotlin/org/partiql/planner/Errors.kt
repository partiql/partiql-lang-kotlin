package org.partiql.planner

import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemSeverity
import org.partiql.plan.Identifier
import org.partiql.planner.internal.typer.toRuntimeType
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingPath
import org.partiql.types.StaticType
import org.partiql.value.PartiQLType
import org.partiql.value.PartiQLValueExperimental

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

    public data class UndefinedVariable(val id: BindingPath) :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            {
                val caseSensitive = id.steps.any { it.case == BindingCase.SENSITIVE }
                "Undefined variable '${id.key}'." +
                    quotationHint(caseSensitive)
            }
        )

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
        val expectedTypes: Set<PartiQLType>,
        val actualType: PartiQLType,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        "Unexpected type $actualType, expected one of ${expectedTypes.joinToString()}"
    }) {

        @OptIn(PartiQLValueExperimental::class)
        public constructor(actualType: StaticType, expectedTypes: Set<StaticType>) : this(
            expectedTypes = expectedTypes.map { PartiQLType.fromLegacy(it.toRuntimeType()) }.toSet(),
            actualType = PartiQLType.fromLegacy(actualType.toRuntimeType())
        )
    }

    public data class UnknownFunction(
        val args: List<PartiQLType>,
        val identifier: String,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        val types = args.joinToString { "<${it.toString().lowercase()}>" }
        "Unknown function `$identifier($types)"
    }) {

        @OptIn(PartiQLValueExperimental::class)
        public constructor(identifier: String, args: List<StaticType>) : this(
            args.map { PartiQLType.fromLegacy(it.toRuntimeType()) },
            identifier
        )
    }

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

private fun Identifier.sql(): String = when (this) {
    is Identifier.Qualified -> this.sql()
    is Identifier.Symbol -> this.sql()
}

private fun Identifier.Qualified.sql(): String = root.sql() + "." + steps.joinToString(".") { it.sql() }

private fun Identifier.Symbol.sql(): String = when (caseSensitivity) {
    Identifier.CaseSensitivity.SENSITIVE -> "\"$symbol\""
    Identifier.CaseSensitivity.INSENSITIVE -> symbol
}
