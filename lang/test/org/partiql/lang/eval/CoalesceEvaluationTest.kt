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

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat

class CoalesceEvaluationTest : EvaluatorTestBase() {
    // Note that we can't use `assertEval` for the following tests because it requires expected result
    // to be expressed in Ion, which doesn't currently disambiguate between `MISSING` and `NULL`.
    // Note that `ExprValue.ionValue` *does* add the `$partiql_missing::` annotation -- however this is
    // currently being removed before the assertions are made so we don't have to update thousands of test
    // to include `$partiql_missing::` in their expected results.

    data class CoalesceTestCase(
        val args: List<String>,
        val expectedLegacyResult: String,
        val expectedPermissiveResult: String = expectedLegacyResult
    )

    @ParameterizedTest
    @MethodSource("coalesceEvaluationTests")
    fun runTests(tc: CoalesceTestCase) =
        runEvaluatorTestCase(
            "coalesce(${tc.args.joinToString(",")})",
            expectedResult = tc.expectedLegacyResult,
            expectedPermissiveModeResult = tc.expectedPermissiveResult,
            expectedResultFormat = ExpectedResultFormat.PARTIQL
        )

    companion object {
        fun testCase(
            vararg args: String,
            expectedLegacyResult: String,
            expectedPermissiveResult: String = expectedLegacyResult
        ) =
            CoalesceTestCase(args.toList(), expectedLegacyResult, expectedPermissiveResult)

        @JvmStatic
        @Suppress("unused")
        fun coalesceEvaluationTests() = listOf(
            testCase(
                "null",
                expectedLegacyResult = "null"
            ),
            testCase(
                "null", "null",
                expectedLegacyResult = "null"
            ),
            testCase(
                "missing", "null", "missing",
                expectedLegacyResult = "null"
            ),
            testCase(
                "null", "missing",
                expectedLegacyResult = "null"
            ),
            testCase(
                "missing",
                expectedLegacyResult = "null",
                expectedPermissiveResult = "missing"
            ),
            testCase(
                "missing", "missing",
                expectedLegacyResult = "null",
                expectedPermissiveResult = "missing"
            ),
            testCase(
                "1", "null",
                expectedLegacyResult = "1"
            ),
            testCase(
                "null", "2",
                expectedLegacyResult = "2"
            ),
            testCase(
                "1", "missing",
                expectedLegacyResult = "1"
            ),
            testCase(
                "missing", "2",
                expectedLegacyResult = "2"
            ),
            testCase(
                "null", "missing", "null", "null", "2", "3", "4", "5",
                expectedLegacyResult = "2"
            ),
            testCase(
                "null", "null", "2", "3", "4", "5",
                expectedLegacyResult = "2"
            ),
            testCase(
                "missing", "missing", "2", "3", "4", "5",
                expectedLegacyResult = "2"
            ),
            testCase(
                "null", "missing", "null", "null", "2 in [1,2,3]", "3", "4", "5",
                expectedLegacyResult = "true"
            )
        )
    }
}
