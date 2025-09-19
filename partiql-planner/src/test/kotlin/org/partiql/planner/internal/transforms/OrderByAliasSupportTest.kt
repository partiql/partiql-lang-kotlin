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
