/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.planner.internal.transforms

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.parser.PartiQLParser
import kotlin.test.assertEquals

class OrderByAliasSupportTest {

    data class TestCase(
        val name: String,
        val sql: String,
        val transformedSql: String,
        val description: String
    )

    companion object {
        @JvmStatic
        fun testCases() = listOf(
            TestCase(
                name = "regular_alias",
                sql = """
                    SELECT col AS alias
                    FROM t
                    ORDER BY alias
                """,
                transformedSql = """
                    SELECT col AS alias
                    FROM t
                    ORDER BY col
                """,
                description = "Regular column alias should resolve to original column"
            ),
            TestCase(
                name = "aggregation_alias",
                sql = """
                    SELECT MAX(price) AS max_price
                    FROM products
                    GROUP BY category
                    ORDER BY max_price
                """,
                transformedSql = """
                    SELECT MAX(price) AS max_price
                    FROM products
                    GROUP BY category
                    ORDER BY MAX(price)
                """,
                description = "Aggregation alias should resolve to original aggregation function"
            ),
            TestCase(
                name = "nested_query",
                sql = """
                    SELECT pid AS p
                    FROM (
                        SELECT productId AS pid
                        FROM products
                        ORDER BY pid
                    )
                    ORDER BY p
                """,
                transformedSql = """
                    SELECT pid AS p
                    FROM (
                        SELECT productId AS pid
                        FROM products
                        ORDER BY productId
                    )
                    ORDER BY pid
                """,
                description = "Nested query alias should resolve correctly at each scope level"
            ),
            TestCase(
                name = "multiple_aliases",
                sql = """
                    SELECT col1 AS a, col2 AS b
                    FROM t
                    ORDER BY b, a
                """,
                transformedSql = """
                    SELECT col1 AS a, col2 AS b
                    FROM t
                    ORDER BY col2, col1
                """,
                description = "Multiple aliases should resolve in correct order"
            ),
            TestCase(
                name = "no_alias",
                sql = """
                    SELECT col
                    FROM t
                    ORDER BY col
                """,
                transformedSql = """
                    SELECT col
                    FROM t
                    ORDER BY col
                """,
                description = "No alias case should remain unchanged"
            ),
            // Test cases from https://github.com/partiql/partiql-lang-kotlin/blob/v0.14.9/partiql-lang/src/test/kotlin/org/partiql/lang/eval/visitors/OrderBySortSpecVisitorTransformTests.kt
            TestCase(
                name = "simplest_case",
                sql = """
                    SELECT a AS b
                    FROM foo
                    ORDER BY b
                """,
                transformedSql = """
                    SELECT a AS b
                    FROM foo
                    ORDER BY a
                """,
                description = "Simplest case of alias resolution"
            ),
            TestCase(
                name = "different_projection_aliases",
                sql = """
                    SELECT a AS b
                    FROM (
                      SELECT c AS d
                      FROM e
                      ORDER BY d
                    )
                    ORDER BY b
                """,
                transformedSql = """
                    SELECT a AS b
                    FROM (
                      SELECT c AS d
                      FROM e
                      ORDER BY c
                    )
                    ORDER BY a
                """,
                description = "Different projection aliases in nested queries"
            ),
            TestCase(
                name = "same_projection_alias",
                sql = """
                    SELECT a AS b
                    FROM (
                      SELECT c AS b
                      FROM e
                      ORDER BY b
                    )
                    ORDER BY b
                """,
                transformedSql = """
                    SELECT a AS b
                    FROM (
                      SELECT c AS b
                      FROM e
                      ORDER BY c
                    )
                    ORDER BY a
                """,
                description = "Same projection alias in nested queries"
            ),
            TestCase(
                name = "complex_projection_same_alias",
                sql = """
                    SELECT a + b AS b
                    FROM (
                      SELECT b + a AS b
                      FROM e
                      ORDER BY b
                    )
                    ORDER BY b
                """,
                transformedSql = """
                    SELECT a + b AS b
                    FROM (
                      SELECT b + a AS b
                      FROM e
                      ORDER BY b + a
                    )
                    ORDER BY a + b
                """,
                description = "Complex projection expressions with same alias"
            ),
            TestCase(
                name = "case_sensitive_alias",
                sql = """
                    SELECT a + b AS b
                    FROM (
                      SELECT b + a AS b
                      FROM e
                      ORDER BY b
                    )
                    ORDER BY "b"
                """,
                transformedSql = """
                    SELECT a + b AS b
                    FROM (
                      SELECT b + a AS b
                      FROM e
                      ORDER BY b + a
                    )
                    ORDER BY a + b
                """,
                description = "Case sensitive ORDER BY with lowercase projection alias"
            ),
            TestCase(
                name = "case_sensitive_projection",
                sql = """
                    SELECT a + b AS "B"
                    FROM (
                      SELECT b + a AS "B"
                      FROM e
                      ORDER BY b
                    )
                    ORDER BY b
                """,
                transformedSql = """
                    SELECT a + b AS "B"
                    FROM (
                      SELECT b + a AS "B"
                      FROM e
                      ORDER BY b + a
                    )
                    ORDER BY a + b
                """,
                description = "Case sensitive projection alias with case insensitive ORDER BY"
            ),
            TestCase(
                name = "case_insensitive_mismatch",
                sql = """
                    SELECT a + b AS b
                    FROM (
                      SELECT b + a AS b
                      FROM e
                      ORDER BY b
                    )
                    ORDER BY "B"
                """,
                transformedSql = """
                    SELECT a + b AS b
                    FROM (
                      SELECT b + a AS b
                      FROM e
                      ORDER BY b + a
                    )
                    ORDER BY "B"
                """,
                description = "Case insensitive projection with case sensitive ORDER BY - no match"
            ),
            TestCase(
                name = "case_insensitive_different_cases",
                sql = """
                    SELECT a + b AS b
                    FROM (
                      SELECT b + a AS b
                      FROM e
                      ORDER BY b
                    )
                    ORDER BY B
                """,
                transformedSql = """
                    SELECT a + b AS b
                    FROM (
                      SELECT b + a AS b
                      FROM e
                      ORDER BY b + a
                    )
                    ORDER BY a + b
                """,
                description = "Case insensitive aliases with different cases in ORDER BY"
            ),
            TestCase(
                name = "multiple_sort_specs",
                sql = """
                    SELECT a + b AS b
                    FROM (
                      SELECT b + a AS b
                      FROM e
                      ORDER BY b
                    )
                    ORDER BY B, a, b
                """,
                transformedSql = """
                    SELECT a + b AS b
                    FROM (
                      SELECT b + a AS b
                      FROM e
                      ORDER BY b + a
                    )
                    ORDER BY a + b, a, a + b
                """,
                description = "Multiple sort specifications with aliases"
            ),
            TestCase(
                name = "negative_expression_alias",
                sql = """
                    SELECT (a * -1) AS a
                    FROM << { 'a': 1 }, { 'a': 2 } >>
                    ORDER BY a
                """,
                transformedSql = """
                    SELECT (a * -1) AS a
                    FROM << { 'a': 1 }, { 'a': 2 } >>
                    ORDER BY (a * -1)
                """,
                description = "Negative expression with alias matching original column"
            ),
            // Set operator test cases
            TestCase(
                name = "union_with_order_by_alias",
                sql = """
                    SELECT col AS alias FROM t1
                    UNION
                    SELECT col AS alias FROM t2
                    ORDER BY alias
                """,
                transformedSql = """
                    SELECT col AS alias FROM t1
                    UNION
                    SELECT col AS alias FROM t2
                    ORDER BY col
                """,
                description = "UNION with ORDER BY alias resolution"
            ),
            TestCase(
                name = "intersect_with_order_by_alias",
                sql = """
                    SELECT price AS p FROM products
                    INTERSECT
                    SELECT cost AS p FROM orders
                    ORDER BY p
                """,
                transformedSql = """
                    SELECT price AS p FROM products
                    INTERSECT
                    SELECT cost AS p FROM orders
                    ORDER BY price
                """,
                description = "INTERSECT with ORDER BY alias resolution"
            ),
            TestCase(
                name = "except_with_order_by_alias",
                sql = """
                    SELECT id AS identifier FROM table1
                    EXCEPT
                    SELECT id AS identifier FROM table2
                    ORDER BY identifier
                """,
                transformedSql = """
                    SELECT id AS identifier FROM table1
                    EXCEPT
                    SELECT id AS identifier FROM table2
                    ORDER BY id
                """,
                description = "EXCEPT with ORDER BY alias resolution"
            ),
            TestCase(
                name = "union_all_with_complex_alias",
                sql = """
                    SELECT a + b AS total FROM t1
                    UNION ALL
                    SELECT x + y AS total FROM t2
                    ORDER BY total DESC
                """,
                transformedSql = """
                    SELECT a + b AS total FROM t1
                    UNION ALL
                    SELECT x + y AS total FROM t2
                    ORDER BY a + b DESC
                """,
                description = "UNION ALL with complex expression alias resolution"
            )
        )
    }

    @ParameterizedTest
    @MethodSource("testCases")
    fun testOrderByAliasResolution(testCase: TestCase) {
        // Parse original SQL to AST
        val parser = PartiQLParser.standard()
        val originalStatement = parser.parse(testCase.sql.trimIndent())

        // Apply OrderByAliasSupport transform
        val transformedStatement = OrderByAliasSupport.apply(originalStatement.statements[0])

        // Parse expected SQL to AST for comparison
        val expectedStatement = parser.parse(testCase.transformedSql.trimIndent()).statements[0]

        assertEquals(expectedStatement, transformedStatement, testCase.description)
    }
}
