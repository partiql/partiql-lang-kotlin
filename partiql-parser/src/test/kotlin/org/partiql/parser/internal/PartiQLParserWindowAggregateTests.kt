/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

@file:Suppress("DEPRECATION")

package org.partiql.parser.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.exprWindowFunction
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.query
import org.partiql.ast.Ast.sort
import org.partiql.ast.Ast.windowPartition
import org.partiql.ast.Ast.windowSpecification
import org.partiql.ast.AstNode
import org.partiql.ast.Identifier
import org.partiql.ast.SetQuantifier
import org.partiql.ast.WindowFunctionAggregateName
import org.partiql.ast.WindowFunctionType
import org.partiql.ast.expr.Expr
import org.partiql.ast.sql.sql
import org.partiql.parser.PartiQLParser
import kotlin.test.assertEquals

/**
 * Tests for the aggregate window functions COUNT/SUM/AVG/MIN/MAX (e.g. `SUM(x) OVER (...)`).
 */
class PartiQLParserWindowAggregateTests {

    private val parser = PartiQLParser.standard()

    private fun v(name: String): Expr = exprVarRef(Identifier.regular(name), isQualified = false)

    private fun over(type: WindowFunctionType) = exprWindowFunction(
        type = type,
        spec = windowSpecification(
            null,
            listOf(windowPartition(Identifier.regular("g"))),
            orderBy(listOf(sort(v("k"), null, null)))
        )
    )

    private fun assertExpression(input: String, expected: AstNode) {
        val result = parser.parse(input)
        assertEquals(1, result.statements.size)
        assertEquals(query(expected as Expr), result.statements[0])
    }

    /**
     * Asserts that [input] parses, and that printing and re-parsing it produces the same AST.
     */
    private fun assertRoundTrip(input: String) {
        val ast = parser.parse(input).statements.single()
        val printed = ast.sql()
        val reparsed = parser.parse(printed).statements.single()
        assertEquals(ast, reparsed, "Round trip of `$input` through `$printed` changed the AST")
    }

    @Test
    fun countStar() = assertExpression(
        "COUNT(*) OVER (PARTITION BY g ORDER BY k)",
        over(WindowFunctionType.Aggregate(WindowFunctionAggregateName.COUNT(), null, null))
    )

    @Test
    fun countExpr() = assertExpression(
        "COUNT(x) OVER (PARTITION BY g ORDER BY k)",
        over(WindowFunctionType.Aggregate(WindowFunctionAggregateName.COUNT(), null, v("x")))
    )

    @Test
    fun sum() = assertExpression(
        "SUM(x) OVER (PARTITION BY g ORDER BY k)",
        over(WindowFunctionType.Aggregate(WindowFunctionAggregateName.SUM(), null, v("x")))
    )

    @Test
    fun avgCaseInsensitive() = assertExpression(
        "avg(x) OVER (PARTITION BY g ORDER BY k)",
        over(WindowFunctionType.Aggregate(WindowFunctionAggregateName.AVG(), null, v("x")))
    )

    @Test
    fun minAll() = assertExpression(
        "MIN(ALL x) OVER (PARTITION BY g ORDER BY k)",
        over(WindowFunctionType.Aggregate(WindowFunctionAggregateName.MIN(), SetQuantifier.ALL(), v("x")))
    )

    @Test
    fun maxDistinct() = assertExpression(
        "MAX(DISTINCT x) OVER (PARTITION BY g ORDER BY k)",
        over(WindowFunctionType.Aggregate(WindowFunctionAggregateName.MAX(), SetQuantifier.DISTINCT(), v("x")))
    )

    @Test
    fun emptyWindowSpecification() = assertExpression(
        "SUM(x) OVER ()",
        exprWindowFunction(
            type = WindowFunctionType.Aggregate(WindowFunctionAggregateName.SUM(), null, v("x")),
            spec = windowSpecification(null, null, null)
        )
    )

    @Test
    fun nestedGroupAggregateArgument() = assertExpression(
        "SUM(SUM(x)) OVER (PARTITION BY g ORDER BY k)",
        over(
            WindowFunctionType.Aggregate(
                WindowFunctionAggregateName.SUM(),
                null,
                exprCall(Identifier.regular("SUM"), listOf(v("x")), null)
            )
        )
    )

    @Test
    fun aggregateWithoutOverIsAFunctionCall() = assertExpression(
        "SUM(x)",
        exprCall(Identifier.regular("SUM"), listOf(v("x")), null)
    )

    @Test
    fun countStarWithoutOverIsAFunctionCall() = assertExpression(
        "COUNT(*)",
        exprCall(Identifier.regular("COUNT"), emptyList(), null)
    )

    @Test
    fun distinctAggregateWithoutOverIsAFunctionCall() = assertExpression(
        "AVG(DISTINCT x)",
        exprCall(Identifier.regular("AVG"), listOf(v("x")), SetQuantifier.DISTINCT())
    )

    @ParameterizedTest
    @ValueSource(
        strings = [
            "COUNT(*) OVER (PARTITION BY g ORDER BY k)",
            "COUNT(x) OVER (PARTITION BY g ORDER BY k DESC)",
            "COUNT(DISTINCT x) OVER (PARTITION BY g)",
            "SUM(x) OVER ()",
            "SUM(ALL x) OVER w",
            "AVG(x + 1) OVER (ORDER BY k)",
            "MIN(x) OVER (PARTITION BY g)",
            "MAX(DISTINCT x) OVER (PARTITION BY g)",
            "SELECT g, SUM(SUM(x)) OVER (ORDER BY g) AS s, COUNT(*) OVER () FROM t GROUP BY g",
            "SELECT SUM(x) OVER w AS a, ROW_NUMBER() OVER w AS b FROM t WINDOW w AS (PARTITION BY g ORDER BY k)",
        ]
    )
    fun roundTrip(input: String) = assertRoundTrip(input)

    @ParameterizedTest
    @ValueSource(
        strings = [
            // User-defined (or any other) aggregates cannot be used as window functions.
            "my_agg(x) OVER ()",
            "EVERY(x) OVER ()",
            "my_catalog.SUM(x) OVER ()",
            // SQL:2011 does not allow a null treatment on aggregates.
            "SUM(x) IGNORE NULLS OVER ()",
            "SUM(x) RESPECT NULLS OVER ()",
            // Only COUNT accepts `*`, and the aggregates take exactly one argument.
            "SUM(*) OVER ()",
            "COUNT(DISTINCT *) OVER ()",
            "SUM() OVER ()",
            "SUM(x, y) OVER ()",
        ]
    )
    fun invalid(input: String) {
        assertThrows<Exception> { parser.parse(input) }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "SELECT count, sum, avg, min, max FROM t",
            "SELECT t.count, t.sum FROM t AS t",
            "SELECT x AS sum, y AS max FROM t",
            "SELECT VALUE sum FROM t AS sum",
        ]
    )
    fun aggregateNamesAreStillIdentifiers(input: String) {
        parser.parse(input)
    }
}
