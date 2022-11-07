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

package org.partiql.lang.eval.visitors

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.visitors.AggregationVisitorTransform.Companion.GROUP_DELIMITER
import org.partiql.lang.eval.visitors.AggregationVisitorTransform.Companion.GROUP_PREFIX
import org.partiql.lang.util.ArgumentsProviderBase

internal class AggregationVisitorTransformTests : VisitorTransformTestBase() {

    companion object {
        private fun uniqueId(level: Int, index: Int): String = "\"$GROUP_PREFIX$level$GROUP_DELIMITER$index\""
    }

    @ParameterizedTest
    @ArgumentsSource(ValidArgumentsProvider::class)
    internal fun validTests(tc: TransformTestCase) = runTestForIdempotentTransform(tc, AggregationVisitorTransform())

    @ParameterizedTest
    @ArgumentsSource(ErrorArgumentsProvider::class)
    internal fun errorTests(tc: TransformErrorTestCase) = runErrorTest(tc, AggregationVisitorTransform())

    internal class ValidArgumentsProvider : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            TransformTestCase(
                name = "Using explicit and implicit keys",
                original = """
                    SELECT k AS outputKey
                    FROM t
                    GROUP BY t.a AS k
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS outputKey
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                """
            ),
            TransformTestCase(
                name = "Grabbing using implicit key",
                original = """
                    SELECT a AS outputKey
                    FROM t
                    GROUP BY t.a
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS outputKey
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                """
            ),
            TransformTestCase(
                name = "Using explicit and implicit keys",
                original = """
                    SELECT k AS output1, b AS output2
                    FROM t
                    GROUP BY t.a AS k, t.b
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS output1, ${uniqueId(0, 1)} AS output2
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}, t.b AS ${uniqueId(0, 1)}
                """
            ),
            TransformTestCase(
                name = "Using explicit keys with aggregate functions",
                original = """
                    SELECT k AS output1, SUM(k) AS output2
                    FROM t
                    GROUP BY t.a AS k
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS output1, SUM(t.a) AS output2
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                """
            ),
            TransformTestCase(
                name = "Using implicit keys with aggregate functions",
                original = """
                    SELECT a AS output1, SUM(a) AS output2
                    FROM t
                    GROUP BY t.a
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS output1, SUM(t.a) AS output2
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                """
            ),
            TransformTestCase(
                name = "Explicitly using source",
                original = """
                    SELECT SUM(t.a) AS output2
                    FROM t
                    GROUP BY t.a
                """,
                expected = """
                    SELECT SUM(t.a) AS output2
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                """
            ),
            TransformTestCase(
                name = "Shadowing where inner and outer have aggregations",
                original = """
                    SELECT k AS output1, SUM(k) AS output2
                    FROM (
                        SELECT k AS k, SUM(k) AS output2
                        FROM t
                        GROUP BY t.a AS g, t.b AS k
                    )
                    GROUP BY t.a AS k
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS output1, SUM(t.a) AS output2
                    FROM (
                        SELECT ${uniqueId(0, 1)} AS k, SUM(t.b) AS output2
                        FROM t
                        GROUP BY t.a AS ${uniqueId(0, 0)}, t.b AS ${uniqueId(0, 1)}
                    )
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                """
            ),
            TransformTestCase(
                name = "Shadowing where outer doesn't have aggregations",
                original = """
                    SELECT k AS output1
                    FROM (
                        SELECT k AS k, SUM(k) AS output2
                        FROM t
                        GROUP BY t.a AS g, t.b AS k
                    )
                """,
                expected = """
                    SELECT k AS output1
                    FROM (
                        SELECT ${uniqueId(0, 1)} AS k, SUM(t.b) AS output2
                        FROM t
                        GROUP BY t.a AS ${uniqueId(0, 0)}, t.b AS ${uniqueId(0, 1)}
                    )
                """
            ),
            TransformTestCase(
                name = "Shadowing where inner doesn't have aggregations",
                original = """
                    SELECT k AS output1, SUM(k) AS output2
                    FROM (
                        SELECT k AS k
                        FROM t
                    )
                    GROUP BY t.a AS k
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS output1, SUM(t.a) AS output2
                    FROM (
                        SELECT k AS k
                        FROM t
                    )
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                """
            ),
            TransformTestCase(
                name = "Sub-query in projection without grouping",
                original = """
                    SELECT
                        k + SUM(k) AS output1,
                        (
                            SELECT k + SUM(k) AS output2
                            FROM t
                        ) AS subQueryOutput
                    FROM t
                    GROUP BY t.a AS k
                """,
                expected = """
                    SELECT
                        ${uniqueId(0, 0)} + SUM(t.a) AS output1,
                        (
                            SELECT ${uniqueId(0, 0)} + SUM(${uniqueId(0, 0)}) AS output2
                            FROM t
                        ) AS subQueryOutput
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                """
            ),
            TransformTestCase(
                name = "Sub-query in projection with grouping",
                original = """
                    SELECT
                        k + SUM(k) AS output1,
                        (
                            SELECT k + SUM(k) AS output2
                            FROM t
                            GROUP BY t.a AS h, t.b AS k
                        ) AS subQueryOutput
                    FROM t
                    GROUP BY t.a AS k
                """,
                expected = """
                    SELECT
                        ${uniqueId(0, 0)} + SUM(t.a) AS output1,
                        (
                            SELECT ${uniqueId(1, 1)} + SUM(t.b) AS output2
                            FROM t
                            GROUP BY t.a AS ${uniqueId(1, 0)}, t.b AS ${uniqueId(1, 1)}
                        ) AS subQueryOutput
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                """
            ),
            TransformTestCase(
                name = "Sub-query in projection without outer explicit grouping",
                original = """
                    SELECT
                        SUM(k) AS output1,
                        (
                            SELECT k + SUM(k) AS output2
                            FROM t
                            GROUP BY t.a AS h, t.b AS k
                        ) AS subQueryOutput
                    FROM t
                """,
                expected = """
                    SELECT
                        SUM(k) AS output1,
                        (
                            SELECT ${uniqueId(1, 1)} + SUM(t.b) AS output2
                            FROM t
                            GROUP BY t.a AS ${uniqueId(1, 0)}, t.b AS ${uniqueId(1, 1)}
                        ) AS subQueryOutput
                    FROM t
                """
            ),
            TransformTestCase(
                name = "Sub-query in projection without outer grouping",
                original = """
                    SELECT
                        k AS output1,
                        (
                            SELECT k + SUM(k) AS output2
                            FROM t
                            GROUP BY t.a AS h, t.b AS k
                        ) AS subQueryOutput
                    FROM t
                """,
                expected = """
                    SELECT
                        k AS output1,
                        (
                            SELECT ${uniqueId(1, 1)} + SUM(t.b) AS output2
                            FROM t
                            GROUP BY t.a AS ${uniqueId(1, 0)}, t.b AS ${uniqueId(1, 1)}
                        ) AS subQueryOutput
                    FROM t
                """
            ),
            TransformTestCase(
                name = "Sub-query in projection with grouping (and group as)",
                original = """
                    SELECT
                        k + SUM(k) + g AS output1,
                        (
                            SELECT k + SUM(k) + g AS output2
                            FROM t
                            GROUP BY t.a AS h, t.b AS k
                            GROUP AS g
                        ) AS subQueryOutput
                    FROM t
                    GROUP BY t.a AS k
                    GROUP AS g
                """,
                expected = """
                    SELECT
                        ${uniqueId(0, 0)} + SUM(t.a) + "g" AS output1,
                        (
                            SELECT ${uniqueId(1, 1)} + SUM(t.b) + "g" AS output2
                            FROM t
                            GROUP BY t.a AS ${uniqueId(1, 0)}, t.b AS ${uniqueId(1, 1)}
                            GROUP AS g
                        ) AS subQueryOutput
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                    GROUP AS g
                """
            ),
            TransformTestCase(
                name = "Triple nested sub-query in projection with grouping (and group as)",
                original = """
                    SELECT
                        k + SUM(k) + "g" AS output1,
                        (
                            SELECT k + SUM(k) + "g" AS output2,
                            (
                                SELECT k + SUM(k) + "g" AS output3
                                FROM t
                                GROUP BY t.a AS z, t.b AS y, t.c AS k
                                GROUP AS g
                            ) AS thirdNest
                            FROM t
                            GROUP BY t.a AS h, t.b AS k
                            GROUP AS g
                        ) AS subQueryOutput
                    FROM t
                    GROUP BY t.a AS k
                    GROUP AS g
                """,
                expected = """
                    SELECT
                        ${uniqueId(0, 0)} + SUM(t.a) + "g" AS output1,
                        (
                            SELECT ${uniqueId(1, 1)} + SUM(t.b) + "g" AS output2,
                            (
                                SELECT ${uniqueId(2, 2)} + SUM(t.c) + "g" AS output3
                                FROM t
                                GROUP BY t.a AS ${uniqueId(2, 0)}, t.b AS ${uniqueId(2, 1)}, t.c AS ${uniqueId(2, 2)}
                                GROUP AS g
                            ) AS thirdNest
                            FROM t
                            GROUP BY t.a AS ${uniqueId(1, 0)}, t.b AS ${uniqueId(1, 1)}
                            GROUP AS g
                        ) AS subQueryOutput
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                    GROUP AS g
                """
            ),
            TransformTestCase(
                name = "Triple nest, but we're looking in the parent scopes (most-inner-scope doesn't have groups)",
                original = """
                    SELECT
                        k + SUM(k) + g AS output1,
                        (
                            SELECT k + SUM(k) + g AS output2,
                            (
                                SELECT z + k + SUM(k) + g AS output3
                                FROM t
                            ) AS thirdNest
                            FROM t
                            GROUP BY t.a AS h, t.b AS k
                            GROUP AS g
                        ) AS subQueryOutput
                    FROM t
                    GROUP BY t.a AS k, t.b AS z
                    GROUP AS g
                """,
                expected = """
                    SELECT
                        ${uniqueId(0, 0)} + SUM(t.a) + "g" AS output1,
                        (
                            SELECT ${uniqueId(1, 1)} + SUM(t.b) + "g" AS output2,
                            (
                                SELECT ${uniqueId(0, 1)} + ${uniqueId(1, 1)} + SUM(${uniqueId(1, 1)}) + "g" AS output3
                                FROM t
                            ) AS thirdNest
                            FROM t
                            GROUP BY t.a AS ${uniqueId(1, 0)}, t.b AS ${uniqueId(1, 1)}
                            GROUP AS g
                        ) AS subQueryOutput
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}, t.b AS ${uniqueId(0, 1)}
                    GROUP AS g
                """
            ),
            TransformTestCase(
                name = "Simple having",
                original = """
                    SELECT attributeId AS attrId, SUM(attributeId) as the_sum
                    FROM t
                    GROUP BY attributeId
                    HAVING SUM(attributeId) >= 160
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS attrId, SUM(attributeId) as the_sum
                    FROM t
                    GROUP BY attributeId AS ${uniqueId(0, 0)}
                    HAVING SUM(attributeId) >= 160
                """
            ),
            TransformTestCase(
                name = "Complex query with nesting in PROJ, HAVING, and ORDER BY",
                original = """
                    SELECT
                        k + SUM(k) + g AS output1,
                        (
                            SELECT k + SUM(k) + g AS output2
                            FROM t
                            GROUP BY t.a AS h, t.b AS k
                            GROUP AS g
                        ) AS subQueryOutput
                    FROM t
                    GROUP BY t.a AS k
                    GROUP AS g
                    HAVING
                        k + SUM(k) + (
                            SELECT k + SUM(k) AS out1
                            FROM t
                        ) > 0
                    ORDER BY
                        k + SUM(k) + (
                            SELECT k + SUM(k) AS out2
                            FROM t
                            GROUP BY t.b AS k
                        )
                """,
                expected = """
                    SELECT
                        ${uniqueId(0, 0)} + SUM(t.a) + "g" AS output1,
                        (
                            SELECT ${uniqueId(1, 1)} + SUM(t.b) + "g" AS output2
                            FROM t
                            GROUP BY t.a AS ${uniqueId(1, 0)}, t.b AS ${uniqueId(1, 1)}
                            GROUP AS g
                        ) AS subQueryOutput
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                    GROUP AS g
                    HAVING
                        ${uniqueId(0, 0)} + SUM(t.a) + (
                            SELECT ${uniqueId(0, 0)} + SUM(${uniqueId(0, 0)}) AS out1
                            FROM t
                        ) > 0
                    ORDER BY
                        ${uniqueId(0, 0)} + SUM(t.a) + (
                            SELECT ${uniqueId(1, 0)} + SUM(t.b) AS out2
                            FROM t
                            GROUP BY t.b AS ${uniqueId(1, 0)}
                        )
                """
            ),
            TransformTestCase(
                name = "Case sensitive",
                original = """
                    SELECT "K" AS outputKey
                    FROM t
                    GROUP BY t.a AS K
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS outputKey
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                """
            ),
            TransformTestCase(
                name = "Case insensitive",
                original = """
                    SELECT k AS outputKey
                    FROM t
                    GROUP BY t.a AS K
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS outputKey
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                """
            ),
            TransformTestCase(
                name = "Project all",
                original = """
                    SELECT *
                    FROM t
                    GROUP BY t.a AS k, t.b AS G
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS "k", ${uniqueId(0, 1)} AS "G"
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}, t.b AS ${uniqueId(0, 1)}
                """
            ),
            TransformTestCase(
                name = "Nested project all with from",
                original = """
                    SELECT *
                    FROM (
                        SELECT p AS p
                        FROM t
                        GROUP BY t.a AS l, t.b AS p
                    ) AS t
                    GROUP BY t.a AS k
                    HAVING SUM(k) > 2
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS "k"
                    FROM (
                        SELECT ${uniqueId(0, 1)} AS p
                        FROM t
                        GROUP BY t.a AS ${uniqueId(0, 0)}, t.b AS ${uniqueId(0, 1)}
                    ) AS t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                    HAVING SUM(t.a) > 2
                """
            ),
            TransformTestCase(
                name = "Nested project all in from",
                original = """
                    SELECT *
                    FROM (
                        SELECT *
                        FROM t
                        GROUP BY t.a AS l, t.b AS p
                    ) AS t
                    GROUP BY t.a AS k
                    HAVING SUM(k) > 2
                """,
                expected = """
                    SELECT ${uniqueId(0, 0)} AS "k"
                    FROM (
                        SELECT ${uniqueId(0, 0)} AS "l", ${uniqueId(0, 1)} AS "p"
                        FROM t
                        GROUP BY t.a AS ${uniqueId(0, 0)}, t.b AS ${uniqueId(0, 1)}
                    ) AS t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                    HAVING SUM(t.a) > 2
                """
            ),
            TransformTestCase(
                name = "Nested project all in projection",
                original = """
                    SELECT
                        k AS k,
                        (
                            SELECT *
                            FROM t
                            GROUP BY t.a AS l, t.b AS p
                        ) AS t
                    FROM t
                    GROUP BY t.a AS k
                    HAVING SUM(k) > 2
                """,
                expected = """
                    SELECT
                        ${uniqueId(0, 0)} AS k,
                        (
                            SELECT ${uniqueId(1, 0)} AS "l", ${uniqueId(1, 1)} AS "p"
                            FROM t
                            GROUP BY t.a AS ${uniqueId(1, 0)}, t.b AS ${uniqueId(1, 1)}
                        ) AS t
                    FROM t
                    GROUP BY t.a AS ${uniqueId(0, 0)}
                    HAVING SUM(t.a) > 2
                """
            )
        )
    }

    internal class ErrorArgumentsProvider : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            TransformErrorTestCase(
                query = """
                    SELECT h AS h
                    FROM t
                    GROUP BY t.a AS k
                """,
                expectedErrorCode = ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY
            ),
            TransformErrorTestCase(
                query = """
                    SELECT "k" AS h
                    FROM t
                    GROUP BY t.a AS K
                """,
                expectedErrorCode = ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY
            ),
            TransformErrorTestCase(
                query = """
                    SELECT "G" AS h
                    FROM t
                    GROUP BY t.a AS K
                    GROUP AS g
                """,
                expectedErrorCode = ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY
            )
        )
    }
}
