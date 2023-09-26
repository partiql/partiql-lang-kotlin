package org.partiql.lang.planner

import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemSeverity

/**
 * Contains detailed information about errors that may occur during query planning.
 *
 * This information can be used to generate end-user readable error messages and is also easy to assert
 * equivalence in unit tests.
 */
sealed class PlanningProblemDetails(
    override val severity: ProblemSeverity,
    val messageFormatter: () -> String
) : ProblemDetails {

    override fun toString() = message
    override val message: String get() = messageFormatter()

    data class ParseError(val parseErrorMessage: String) :
        PlanningProblemDetails(ProblemSeverity.ERROR, { parseErrorMessage })

    data class CompileError(val errorMessage: String) :
        PlanningProblemDetails(ProblemSeverity.ERROR, { errorMessage })

    data class UndefinedVariable(val variableName: String, val caseSensitive: Boolean) :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            {
                "Undefined variable '$variableName'." +
                    quotationHint(caseSensitive)
            }
        )

    data class UndefinedDmlTarget(val variableName: String, val caseSensitive: Boolean) :
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

    object InvalidExcludeExpr : PlanningProblemDetails(
        severity = ProblemSeverity.ERROR,
        messageFormatter = {
            "Exclusion expression data type mismatch"
        }
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
