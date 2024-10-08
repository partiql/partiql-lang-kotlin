package org.partiql.planner.internal

import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemSeverity
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.Error
import org.partiql.spi.errors.ErrorCode
import org.partiql.spi.errors.Property
import org.partiql.spi.function.Function
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

    open fun toError(line: Int?, column: Int?, length: Int?): Error {
        return Error.of(
            ErrorCode.INTERNAL_ERROR,
            mapOf(
                Property.LINE_NO to line,
                Property.COLUMN_NO to column,
                Property.LENGTH to length
            )
        )
    }

    protected fun Map<Int, Any?>.withLocations(line: Int?, column: Int?, length: Int?): Map<Int, Any?> {
        return mapOf(
            Property.LINE_NO to line,
            Property.COLUMN_NO to column,
            Property.LENGTH to length
        ) + this
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

        override fun toError(line: Int?, column: Int?, length: Int?): Error {
            return Error.of(
                ErrorCode.UNDEFINED_VARIABLE,
                mapOf(
                    Property.IDENTIFIER_CHAIN to name,
                    Property.LOCALS to inScopeVariables,
                ).withLocations(line, column, length)
            )
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

    data class UnimplementedFeature(val featureName: String) : PlanningProblemDetails(
            ProblemSeverity.ERROR,
            { "The syntax at this location is valid but utilizes unimplemented PartiQL feature '$featureName'" }
    ) {
        override fun toError(line: Int?, column: Int?, length: Int?): Error {
            return Error.of(
                ErrorCode.FEATURE_NOT_SUPPORTED,
                mapOf(
                    Property.FEATURE_NAME to featureName
                ).withLocations(line, column, length)
            )
        }
    }

    data class UnexpectedType(
        val actualType: PType,
        val expectedTypes: Set<PType>,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        "Unexpected type $actualType, expected one of ${expectedTypes.joinToString { it.toString() }}"
    }) {
        override fun toError(line: Int?, column: Int?, length: Int?): Error {
            return Error.of(
                ErrorCode.TYPE_MISMATCH,
                mapOf(
                    Property.INPUT_TYPE to actualType,
                    Property.ALLOWED_TYPES to expectedTypes,
                ).withLocations(line, column, length)
            )
        }
    }

    data class UnknownFunction(
        val identifier: Identifier,
        val args: List<PType>,
        val variants: List<Function> = emptyList()
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        val types = args.joinToString { "<${it.toString().lowercase()}>" }
        "Unknown function `$identifier($types)"
    }) {
        override fun toError(line: Int?, column: Int?, length: Int?): Error {
            return Error.of(
                ErrorCode.UNDEFINED_FUNCTION,
                mapOf(
                    Property.IDENTIFIER_CHAIN to identifier,
                    Property.INPUT_ARGUMENT_TYPES to args,
                    Property.FN_VARIANTS to variants
                ).withLocations(line, column, length)
            )
        }
    }

    data class UnknownCast(
        val source: PType,
        val target: PType,
    ) : PlanningProblemDetails(ProblemSeverity.ERROR, {
        "Cast does not exist for $source to $target."
    }) {
        override fun toError(line: Int?, column: Int?, length: Int?): Error {
            return Error.of(
                ErrorCode.UNDEFINED_CAST,
                mapOf(
                    Property.INPUT_TYPE to source,
                    Property.TARGET_TYPE to target,
                ).withLocations(line, column, length)
            )
        }
    }

    data class ExpressionAlwaysReturnsMissing(val reason: String? = null) : PlanningProblemDetails(
        severity = ProblemSeverity.ERROR,
        messageFormatter = { "Expression always returns missing: caused by $reason" }
    ) {
        override fun toError(line: Int?, column: Int?, length: Int?): Error {
            return Error.of(
                ErrorCode.ALWAYS_MISSING,
                emptyMap<Int, Any>().withLocations(line, column, length)
            )
        }
    }

    // TODO: ToError
    data class IncompatibleTypesForOp(
        val actualTypes: List<PType>,
        val operator: String,
    ) :
        PlanningProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "${actualTypes.joinToString { it.toString() }} is/are incompatible data types for the '$operator' operator." }
        )
}
