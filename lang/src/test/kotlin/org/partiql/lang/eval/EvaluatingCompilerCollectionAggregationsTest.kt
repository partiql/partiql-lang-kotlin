/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.builtins.toSession
import org.partiql.lang.eval.evaluatortestframework.EvaluatorErrorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.util.ArgumentsProviderBase

internal class EvaluatingCompilerCollectionAggregationsTest : EvaluatorTestBase() {

    companion object {
        private val SESSION = mapOf(
            "table_05" to """
                [
                    {a: [2, 4], b: 10},
                    {a: [2, 4], b: 20},
                    {a: [6, 7], b: 20}
                ]
            """,
            "table_06" to """
                [
                    {a: [80, 90], b: 55},
                    {a: [80, 90], b: 65},
                    {a: [30, 40], b: 45}
                ]
            """,
        ).toSession()
    }

    @ParameterizedTest
    @ArgumentsSource(ValidTestArguments::class)
    fun validTests(tc: EvaluatorTestCase) {
        val newTc = tc.copy(targetPipeline = EvaluatorTestTarget.PARTIQL_PIPELINE)
        runEvaluatorTestCase(newTc, SESSION)
    }

    @ParameterizedTest
    @ArgumentsSource(ErrorTestArguments::class)
    fun errorTests(tc: EvaluatorErrorTestCase) {
        val newTc = tc.copy(targetPipeline = EvaluatorTestTarget.PARTIQL_PIPELINE)
        runEvaluatorErrorTestCase(newTc, SESSION)
    }

    internal class ValidTestArguments : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            EvaluatorTestCase(
                groupName = "Simple top-level explicit call",
                query = """
                    COLL_SUM('all', [1, 1, 2])
                """,
                expectedResult = "4"
            ),
            EvaluatorTestCase(
                groupName = "Simple top-level explicit call (using distinct)",
                query = """
                    COLL_SUM('distinct', [1, 1, 2])
                """,
                expectedResult = "3"
            ),
            EvaluatorTestCase(
                groupName = "Top-level call with all aggregation functions (using 'all')",
                query = """
                    COLL_SUM('all', [1, 1, 2])       -- 4
                    + COLL_AVG('all', [2, 2, 2])     -- + 2
                    + COLL_MIN('all', [1, 1, 2])     -- + 1
                    + COLL_MAX('all', [1, 1, 2])     -- + 2
                    + COLL_COUNT('all', [1, 1, 2])   -- + 3 = 12
                """,
                expectedResult = "12."
            ),
            EvaluatorTestCase(
                groupName = "Top-level call with all aggregation functions (using 'distinct')",
                query = """
                    COLL_SUM('distinct', [1, 1, 2])       -- 3
                    + COLL_AVG('distinct', [1, 1, 2])     -- + 1.5
                    + COLL_MIN('distinct', [1, 1, 2])     -- + 1
                    + COLL_MAX('distinct', [1, 1, 2])     -- + 2
                    + COLL_COUNT('distinct', [1, 1, 2])   -- + 2 = 9.5
                """,
                expectedResult = "9.5"
            ),
            EvaluatorTestCase(
                groupName = "Simple top-level implicit call (using implicit ALL)",
                query = """
                    SUM([1, 1, 2])
                """,
                expectedResult = "4"
            ),
            EvaluatorTestCase(
                groupName = "Simple top-level implicit call (using explicit ALL)",
                query = """
                    SUM(ALL [1, 1, 2])
                """,
                expectedResult = "4"
            ),
            EvaluatorTestCase(
                groupName = "Simple top-level implicit call (using distinct)",
                query = """
                    SUM(DISTINCT [1, 1, 2])
                """,
                expectedResult = "3"
            ),
            EvaluatorTestCase(
                groupName = "As operand to binary operation",
                query = """
                    1 + COLL_AVG('all', [1, 2])
                """,
                expectedResult = "2.5"
            ),
            EvaluatorTestCase(
                groupName = "Within projection list",
                query = """
                    SELECT k, COLL_SUM('all', k) AS coll_sum_a, SUM(t.b) AS sum_b
                    FROM table_05 AS t
                    GROUP BY t.a AS k
                """,
                expectedResult = """
                    <<
                        {'k': [2, 4], 'coll_sum_a': 6, 'sum_b': 30},
                        {'k': [6, 7], 'coll_sum_a': 13, 'sum_b': 20}
                    >>
                """
            ),
            EvaluatorTestCase(
                groupName = "Within FROM",
                query = """
                    SELECT COLL_SUM('distinct', k) AS new_k, SUM(sum_b) AS total_sum_b
                    FROM (
                        SELECT k AS k, COLL_SUM('all', k) AS coll_sum_a, SUM(t.b) AS sum_b
                        FROM table_05 AS t
                        GROUP BY t.a AS k
                    ) AS nested_t
                    GROUP BY k AS k
                """,
                expectedResult = """
                    <<
                        {'new_k': 6, 'total_sum_b': 30},
                        {'new_k': 13, 'total_sum_b': 20}
                    >>
                """
            ),
            EvaluatorTestCase(
                groupName = "Nested example",
                query = """
                    SELECT
                        k,
                        COLL_SUM('all', k) AS coll_sum_a,
                        SUM(t.b) AS sum_b,
                        (
                            SELECT VALUE COLL_SUM('all', k)
                            FROM [0]
                        ) AS coll_sum_inner
                    FROM table_05 AS t
                    GROUP BY t.a AS k
                """,
                expectedResult = """
                    <<
                        {'k': [2, 4], 'coll_sum_a': 6, 'coll_sum_inner': <<6>>, 'sum_b': 30},
                        {'k': [6, 7], 'coll_sum_a': 13, 'coll_sum_inner': <<13>>, 'sum_b': 20}
                    >>
                """
            ),
            EvaluatorTestCase(
                groupName = "Within ORDER BY clause",
                query = """
                    SELECT k, COLL_SUM('all', k) AS coll_sum_a, SUM(t.b) AS sum_b
                    FROM table_05 AS t
                    GROUP BY t.a AS k
                    ORDER BY COLL_SUM('all', k) DESC
                """,
                expectedResult = """
                    [
                        {'k': [6, 7], 'coll_sum_a': 13, 'sum_b': 20},
                        {'k': [2, 4], 'coll_sum_a': 6, 'sum_b': 30}
                    ]
                """
            ),
            EvaluatorTestCase(
                groupName = "Nested example with ORDER BY clause",
                query = """
                    SELECT
                        k,
                        COLL_SUM('all', k) AS coll_sum_a,
                        SUM(t.b) AS sum_b,
                        (
                            SELECT COLL_AVG('all', k2) AS coll_avg_inner_k, AVG(t.b) AS avg_inner_b
                            FROM table_06 AS t
                            GROUP BY t.a AS k2
                            ORDER BY COLL_AVG('all', k2)
                        ) AS coll_sum_inner
                    FROM table_05 AS t
                    GROUP BY t.a AS k
                """,
                expectedResult = """
                    <<
                        {
                            'k': [2, 4],
                            'coll_sum_a': 6,
                            'coll_sum_inner': [
                                { 'coll_avg_inner_k': 35., 'avg_inner_b': 45. },
                                { 'coll_avg_inner_k': 85., 'avg_inner_b': 60. }
                            ],
                            'sum_b': 30
                        },
                        {
                            'k': [6, 7],
                            'coll_sum_a': 13,
                            'coll_sum_inner': [
                                { 'coll_avg_inner_k': 35., 'avg_inner_b': 45. },
                                { 'coll_avg_inner_k': 85., 'avg_inner_b': 60. }
                            ],
                            'sum_b': 20
                        }
                    >>
                """
            ),
            EvaluatorTestCase(
                groupName = "Within HAVING clause",
                query = """
                    SELECT k, COLL_SUM('all', k) AS coll_sum_a, SUM(t.b) AS sum_b
                    FROM table_05 AS t
                    GROUP BY t.a AS k
                    HAVING COLL_SUM('all', k) > 10
                """,
                expectedResult = """
                    <<
                        {'k': [6, 7], 'coll_sum_a': 13, 'sum_b': 20}
                    >>
                """
            ),
        )
    }

    internal class ErrorTestArguments : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            EvaluatorErrorTestCase(
                groupName = "Lack of set quantifier",
                query = "COLL_SUM([1,2])",
                expectedErrorCode = ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL
            ),
            EvaluatorErrorTestCase(
                groupName = "Lack of argument",
                query = "COLL_SUM('all')",
                expectedErrorCode = ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL
            ),
            EvaluatorErrorTestCase(
                groupName = "Not an actual aggregation function",
                query = "COLL_AVERAGE('all', [1, 2])",
                expectedErrorCode = ErrorCode.EVALUATOR_NO_SUCH_FUNCTION
            ),
            EvaluatorErrorTestCase(
                groupName = "Not using collection of numbers",
                query = "COLL_AVG('all', ['s', 'h'])",
                expectedErrorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION
            ),
            EvaluatorErrorTestCase(
                groupName = "Not using collection of numbers #2",
                query = "COLL_AVG('all', [1, 2, 'h'])",
                expectedErrorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION
            ),
            EvaluatorErrorTestCase(
                groupName = "Not using collection of numbers #3",
                query = "AVG([1, 2, 'h'])",
                expectedErrorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION
            )
        )
    }
}
