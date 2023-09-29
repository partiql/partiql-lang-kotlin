package org.partiql.lang.ast.passes

import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemSeverity
import org.partiql.lang.ast.passes.inference.stringWithoutNullMissing
import org.partiql.types.StaticType

/**
 * Variants of [SemanticProblemDetails] contain info about various problems that can be encountered through semantic
 * passes.
 */
sealed class SemanticProblemDetails(override val severity: ProblemSeverity, val messageFormatter: () -> String) : ProblemDetails {
    override val message: String
        get() = messageFormatter()

    data class IncorrectNumberOfArgumentsToFunctionCall(val functionName: String, val expectedArity: IntRange, val actualArity: Int) :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = {
                "Incorrect number of arguments for '$functionName'. " +
                    "Expected $expectedArity but was supplied $actualArity."
            }
        )

    data class CoercionError(val actualType: StaticType) : SemanticProblemDetails(
        severity = ProblemSeverity.ERROR,
        messageFormatter = {
            "Unable to coerce $actualType into a single value."
        }
    )

    object DuplicateAliasesInSelectListItem :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "Duplicate projection field encountered in SelectListItem expression" }
        )

    data class NoSuchFunction(val functionName: String) :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "No such function '$functionName'" }
        )

    data class IncompatibleDatatypesForOp(val actualArgumentTypes: List<StaticType>, val nAryOp: String) :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "${stringWithoutNullMissing(actualArgumentTypes)} is/are incompatible data types for the '$nAryOp' operator." }
        )

    data class IncompatibleDataTypeForExpr(val expectedType: StaticType, val actualType: StaticType) :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "In this context, $expectedType is expected but expression returns $actualType" }
        )

    object ExpressionAlwaysReturnsNullOrMissing :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "Expression always returns null or missing." }
        )

    data class InvalidArgumentTypeForFunction(val functionName: String, val expectedType: StaticType, val actualType: StaticType) :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "Invalid argument type for $functionName. Expected $expectedType but got $actualType" }
        )

    data class NullOrMissingFunctionArgument(val functionName: String) :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = {
                "Function $functionName given an argument that will always be null or missing. " +
                    "As a result, this function call will always return null or missing."
            }
        )

    object MissingAlias : SemanticProblemDetails(
        severity = ProblemSeverity.ERROR,
        messageFormatter = {
            "Missing a required ALIAS."
        }
    )
}
