package org.partiql.lang.planner.transforms

import org.partiql.lang.errors.ProblemDetails
import org.partiql.lang.errors.ProblemSeverity

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

    override val message: String get() = messageFormatter()

    data class ParseError(val parseErrorMessage: String) :
        PlanningProblemDetails(ProblemSeverity.ERROR, { parseErrorMessage })

    data class CompileError(val parseErrorMessage: String) :
        PlanningProblemDetails(ProblemSeverity.ERROR, { parseErrorMessage })

    data class UndefinedVariable(val variableName: String, val caseSensitive: Boolean) :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            {
                "Undefined variable '$variableName'." +
                    if (caseSensitive) {
                        // Individuals that are new to SQL often try to use double quotes for string literals.
                        // Let's help them out a bit.
                        " Hint: did you intend to use single-quotes (') here?  Remember that double-quotes (\") denote " +
                            "quoted identifiers and single-quotes denote strings."
                    } else {
                        ""
                    }
            }
        )

    data class VariablePreviouslyDefined(val variableName: String) :
        PlanningProblemDetails(
            ProblemSeverity.ERROR,
            { "The variable '$variableName' was previously defined." }
        )
}
