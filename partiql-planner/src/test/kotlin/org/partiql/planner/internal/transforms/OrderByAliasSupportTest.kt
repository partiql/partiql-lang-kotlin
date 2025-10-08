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

import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.parser.PartiQLParser
import org.partiql.spi.Context
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.errors.Severity
import kotlin.test.assertEquals

class OrderByAliasSupportTest {

    data class SuccessTestCase(
        val name: String,
        val sql: String,
        val transformedSql: String,
        val description: String,
    )

    data class FailureTestCase(
        val name: String,
        val sql: String,
        val description: String,
        val exception: PRuntimeException
    )

    companion object {
        @JvmStatic
        fun successTestCases() = listOf(
            SuccessTestCase(
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
            SuccessTestCase(
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
            SuccessTestCase(
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
            SuccessTestCase(
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
            SuccessTestCase(
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
            SuccessTestCase(
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
            SuccessTestCase(
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
            SuccessTestCase(
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
            SuccessTestCase(
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
            SuccessTestCase(
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
            SuccessTestCase(
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
            SuccessTestCase(
                name = "case_sensitive_mismatch",
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
            SuccessTestCase(
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
            SuccessTestCase(
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
            SuccessTestCase(
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
            SuccessTestCase(
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
                    ORDER BY alias
                """,
                description = "UNION with ORDER BY alias resolution"
            ),
            SuccessTestCase(
                name = "union_with_order_by_alias_with_inner_order_by",
                sql = """
                    (SELECT a AS c FROM tA ORDER BY c LIMIT 4 OFFSET 1)
                    UNION
                    (SELECT b AS c FROM tB ORDER BY c LIMIT 4 OFFSET 3)
                    ORDER BY c DESC LIMIT 100 OFFSET 1
                """,
                transformedSql = """
                    (SELECT a AS c FROM tA ORDER BY a LIMIT 4 OFFSET 1)
                    UNION
                    (SELECT b AS c FROM tB ORDER BY b LIMIT 4 OFFSET 3)
                    ORDER BY c DESC LIMIT 100 OFFSET 1
                """,
                description = "UNION with ORDER BY alias resolution"
            ),
            SuccessTestCase(
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
                    ORDER BY p
                """,
                description = "INTERSECT with ORDER BY alias resolution"
            ),
            SuccessTestCase(
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
                    ORDER BY identifier
                """,
                description = "EXCEPT with ORDER BY alias resolution"
            ),
            SuccessTestCase(
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
                    ORDER BY total DESC
                """,
                description = "UNION ALL with complex expression alias resolution"
            )
        )

        @JvmStatic
        fun failureTestCases() = listOf(
            FailureTestCase(
                name = "case_insensitive_ambiguous_alias",
                sql = """
                    SELECT col1 as a, col2 as A
                    FROM t
                    ORDER BY a
                """,
                description = "ambiguous_alias case should remain unchanged",
                exception = PRuntimeException(PError(PError.VAR_REF_AMBIGUOUS, Severity.ERROR(), PErrorKind.SEMANTIC(), null, null))
            ),
            FailureTestCase(
                name = "case_sensitive_ambiguous_alias",
                sql = """
                    SELECT col1 as a, col2 as a
                    FROM t
                    ORDER BY "a"
                """,
                description = "ambiguous_alias case should remain unchanged",
                exception = PRuntimeException(PError(PError.VAR_REF_AMBIGUOUS, Severity.ERROR(), PErrorKind.SEMANTIC(), null, null))
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("successTestCases")
    fun testOrderByAliasResolutionSuccess(testCase: SuccessTestCase) {
        // Parse original SQL to AST
        val parser = PartiQLParser.standard()
        val originalStatement = parser.parse(testCase.sql.trimIndent())
        val ctx = Context.standard()

        // Apply OrderByAliasSupport transform
        val transformedStatement = OrderByAliasSupport(ctx.errorListener).apply(originalStatement.statements[0])

        // Parse expected SQL to AST for comparison
        val expectedStatement = parser.parse(testCase.transformedSql.trimIndent()).statements[0]

        assertEquals(expectedStatement, transformedStatement, testCase.description)
    }

    @ParameterizedTest
    @MethodSource("failureTestCases")
    fun testOrderByAliasResolutionFailed(testCase: FailureTestCase) {
        // Parse original SQL to AST
        val parser = PartiQLParser.standard()
        val originalStatement = parser.parse(testCase.sql.trimIndent())
        val ctx = Context.standard()

        val exception = assertThrows<PRuntimeException> {
            // Apply OrderByAliasSupport transform
            val transformedStatement = OrderByAliasSupport(ctx.errorListener).apply(originalStatement.statements[0])
        }

        assertEquals(testCase.exception.error.code(), exception.error.code())
        assertEquals(testCase.exception.error.kind, exception.error.kind)
        assertEquals(testCase.exception.error.severity, exception.error.severity)
    }
}
