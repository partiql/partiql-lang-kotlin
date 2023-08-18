/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.planner

import org.partiql.errors.ProblemDetails
import org.partiql.errors.ProblemSeverity
import org.partiql.planner.impl.stringWithoutNullMissing
import org.partiql.types.StaticType

/**
 * Variants of [SemanticProblemDetails] contain info about various problems that can be encountered through semantic
 * passes.
 */
public sealed class SemanticProblemDetails(override val severity: ProblemSeverity, public val messageFormatter: () -> String) : ProblemDetails {
    override val message: String
        get() = messageFormatter()

    public data class IncorrectNumberOfArgumentsToFunctionCall(val functionName: String, val expectedArity: IntRange, val actualArity: Int) :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = {
                "Incorrect number of arguments for '$functionName'. " +
                    "Expected $expectedArity but was supplied $actualArity."
            }
        )

    public object DuplicateAliasesInSelectListItem :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "Duplicate projection field encountered in SelectListItem expression" }
        )

    public data class NoSuchFunction(val functionName: String) :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "No such function '$functionName'" }
        )

    public data class IncompatibleDatatypesForOp(val actualArgumentTypes: List<StaticType>, val nAryOp: String) :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "${stringWithoutNullMissing(actualArgumentTypes)} is/are incompatible data types for the '$nAryOp' operator." }
        )

    public data class IncompatibleDataTypeForExpr(val expectedType: StaticType, val actualType: StaticType) :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "In this context, $expectedType is expected but expression returns $actualType" }
        )

    public object ExpressionAlwaysReturnsNullOrMissing :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "Expression always returns null or missing." }
        )

    public data class InvalidArgumentTypeForFunction(val functionName: String, val expectedType: StaticType, val actualType: StaticType) :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = { "Invalid argument type for $functionName. Expected $expectedType but got $actualType" }
        )

    public data class NullOrMissingFunctionArgument(val functionName: String) :
        SemanticProblemDetails(
            severity = ProblemSeverity.ERROR,
            messageFormatter = {
                "Function $functionName given an argument that will always be null or missing. " +
                    "As a result, this function call will always return null or missing."
            }
        )

    public object MissingAlias : SemanticProblemDetails(
        severity = ProblemSeverity.ERROR,
        messageFormatter = {
            "Missing a required ALIAS."
        }
    )
}
