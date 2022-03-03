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

class NullIfEvaluationTest : EvaluatorTestBase() {

    data class NullIfTestCase(
        val expr1: String,
        val expr2: String,
        val expected: String
    )

    @ParameterizedTest
    @MethodSource("nullifEvaluationTests")
    fun runTests(tc: NullIfTestCase) = assertEvalExprValue("nullif(${tc.expr1}, ${tc.expr2})", tc.expected)

    companion object {
        fun testCase(expr1: String, expr2: String, expected: String) = NullIfTestCase(expr1, expr2, expected)

        @JvmStatic
        @Suppress("unused")
        fun nullifEvaluationTests() = listOf(
            testCase("1", "1", "null"),
            testCase("1", "2", "1"),
            testCase("1", "1.0", "null"),
            testCase("1", "'1'", "1"),
            testCase("null", "null", "null"),
            testCase("null", "1", "null"),
            testCase("1", "null", "1"),
            testCase("1", "missing", "1"),
            testCase("missing", "1", "missing"),
            testCase("null", "missing", "null"),
            testCase("missing", "null", "null"),
            testCase("[]", "[]", "null"),
            testCase("{}", "{}", "null"),
            testCase("{}", "[]", "{}"),
            testCase("1 = 1", "2 = 2", "null"),
            testCase("1 in [1,2,3]", "2 in [2,3,4]", "null"),
            testCase("1 in [1,2,3]", "1 in [2,3,4]", "true"),
            testCase("1", "'a'", "1")
        )
    }
}
