/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
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
package org.partiql.lang.eval

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.errors.ErrorBehaviorInPermissiveMode
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.util.ArgumentsProviderBase

class EvaluatingCompilerCastTest : CastTestBase() {
    class ConfiguredCastArguments : ArgumentsProviderBase() {
        override fun getParameters() = allConfiguredTestCases
    }

    class DateTimeConfiguredCastArguments : ArgumentsProviderBase() {
        override fun getParameters() = allConfiguredDateTimeTestCases
    }

    class ConfiguredCastArgumentsSimplified : ArgumentsProviderBase() {
        override fun getParameters() = (allConfiguredDateTimeTestCases + allConfiguredTestCases).filterNot { tc ->
            // Only allow CASTs. Not CAN_CAST and CAN_LOSSLESS_CAST
            tc.castCase.funcName.uppercase().trim() in setOf("CAN_CAST", "CAN_LOSSLESS_CAST", "IS")
        }.filterNot { tc ->
            tc.castCase.type.uppercase().trim() in setOf("NULL", "MISSING")
        }.groupBy { tc ->
            tc.castCase.expression
        }.map { (key, cases) ->
            assertEquals("Cases: $cases", 2, cases.size)
            val lhs = cases[0]
            val rhs = cases[1]
            val expected = lhs.castCase.expected ?: rhs.castCase.expected
            val errorCode = lhs.castCase.expectedErrorCode ?: rhs.castCase.expectedErrorCode
            lhs.copy(castCase = lhs.castCase.copy(expected = expected, expectedErrorCode = errorCode))
        }
    }

    @Test
    fun printAllCastCases() {
        val cases = ConfiguredCastArgumentsSimplified().getParameters().map { tc ->
            val assert = when (tc.castCase.expectedErrorCode) {
                null -> EvaluationTestCase.Assertion.Success(EvaluationTestCase.ALL_MODES, tc.castCase.expected!!)
                else -> when (tc.castCase.expectedErrorCode.errorBehaviorInPermissiveMode) {
                    ErrorBehaviorInPermissiveMode.THROW_EXCEPTION -> EvaluationTestCase.Assertion.Failure(EvaluationTestCase.ALL_MODES)
                    ErrorBehaviorInPermissiveMode.RETURN_MISSING -> EvaluationTestCase.Assertion.Multi(
                        listOf(
                            EvaluationTestCase.Assertion.Success(EvaluationTestCase.COERCE, "\$missing::null"),
                            EvaluationTestCase.Assertion.Failure(EvaluationTestCase.ERROR)
                        )
                    )
                }
            }
            EvaluationTestCase(tc.castCase.expression, tc.castCase.expression, assert)
        }
        EvaluationTestCase.print("casts.ion", cases, emptyMap())
    }

    @ParameterizedTest
    @ArgumentsSource(ConfiguredCastArguments::class)
    fun configuredCast(configuredCastCase: ConfiguredCastCase) = configuredCastCase.assertCase()

    @ParameterizedTest
    @ArgumentsSource(DateTimeConfiguredCastArguments::class)
    fun dateTimeConfiguredCast(dateTimeConfiguredCastCase: ConfiguredCastCase) =
        dateTimeConfiguredCastCase.assertCase(
            ExpectedResultFormat.ION
        )
}
