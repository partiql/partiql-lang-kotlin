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
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.AstNode
import org.partiql.ast.Identifier
import org.partiql.ast.Literal.intNum
import org.partiql.ast.Literal.nul
import org.partiql.ast.Query
import org.partiql.ast.WindowFunctionFromFirstOrLast
import org.partiql.ast.WindowFunctionNullTreatment
import org.partiql.ast.WindowFunctionType
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprWindowFunction
import org.partiql.ast.sql.SqlLayout
import org.partiql.ast.sql.sql
import org.partiql.spi.errors.PRuntimeException
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests the parsing (and the SQL round-trip) of the NTILE, FIRST_VALUE, LAST_VALUE, and NTH_VALUE window functions.
 */
class PartiQLParserWindowFunctionTests {

    private val parser = PartiQLParserDefault()

    class Case(val input: String, val expected: WindowFunctionType) {
        override fun toString(): String = input
    }

    @ParameterizedTest
    @MethodSource("cases")
    fun parseAndRoundTrip(case: Case) {
        val actual = parseSingle(case.input)
        val fn = assertIs<ExprWindowFunction>((actual as Query).expr)
        assertEquals(case.expected, fn.functionType)

        // SQL round-trip
        val sql = actual.sql(SqlLayout.ONELINE)
        val reparsed = parseSingle(sql)
        assertEquals(actual, reparsed, "Round-trip mismatch. Printed SQL: $sql")
    }

    @Test
    fun keywordsAsIdentifiers() {
        // NTILE, FIRST_VALUE, LAST_VALUE, and NTH_VALUE are non-reserved
        parseSingle("SELECT ntile, first_value, last_value, nth_value FROM t")
        parseSingle("SELECT ntile.first_value AS last_value FROM nth_value AS ntile")
        parseSingle("SELECT x FROM t AS first_value WHERE first_value.ntile > 2")
        parseSingle("SELECT NTILE(t.a) AS ntile FROM t") // regular function call
    }

    @Test
    fun keywordsAsFunctionNames() {
        assertCall("ntile(1, 2)", "ntile", 2)
        assertCall("ntile(x)", "ntile", 1)
        assertCall("first_value(x)", "first_value", 1)
        assertCall("last_value(x, y)", "last_value", 2)
        assertCall("nth_value(x, 2)", "nth_value", 2)
        assertCall("NTH_VALUE(x, 2, 3)", "nth_value", 3)
    }

    @Test
    fun invalidSyntax() {
        listOf(
            "NTILE() OVER (ORDER BY a)",
            "NTILE(1, 2) OVER (ORDER BY a)",
            "FIRST_VALUE() OVER (ORDER BY a)",
            "FIRST_VALUE(x, 1) OVER (ORDER BY a)",
            "LAST_VALUE(x) FROM LAST OVER (ORDER BY a)",
            "NTH_VALUE(x) OVER (ORDER BY a)",
            "NTH_VALUE(x, 1) IGNORE NULLS FROM LAST OVER (ORDER BY a)",
            "NTH_VALUE(x, 1) FROM MIDDLE OVER (ORDER BY a)",
        ).forEach { input ->
            assertThrows<PRuntimeException>(input) { parser.parse(input) }
        }
    }

    private fun parseSingle(input: String): AstNode {
        val result = parser.parse(input)
        assertEquals(1, result.statements.size)
        return result.statements[0]
    }

    private fun assertCall(input: String, name: String, arity: Int) {
        val actual = parseSingle(input)
        val call = assertIs<ExprCall>((actual as Query).expr)
        val fn = call.function
        assertEquals(null, fn.qualifier.firstOrNull())
        assertEquals(name, fn.identifier.text.lowercase())
        assertEquals(true, fn.identifier.isRegular)
        assertEquals(arity, call.args.size)
    }

    companion object {

        private fun v(name: String): Expr = exprVarRef(Identifier.regular(name), isQualified = false)

        private fun int(value: Int): Expr = exprLit(intNum(value))

        private val IGNORE = WindowFunctionNullTreatment.IGNORE_NULLS()
        private val RESPECT = WindowFunctionNullTreatment.RESPECT_NULLS()
        private val FROM_FIRST = WindowFunctionFromFirstOrLast.FROM_FIRST()
        private val FROM_LAST = WindowFunctionFromFirstOrLast.FROM_LAST()

        @JvmStatic
        fun cases() = listOf(
            // NTILE
            Case("NTILE(4) OVER (ORDER BY a)", WindowFunctionType.Ntile(int(4))),
            Case("ntile(4) OVER (PARTITION BY p ORDER BY a DESC)", WindowFunctionType.Ntile(int(4))),
            Case("NTILE(NULL) OVER w", WindowFunctionType.Ntile(exprLit(nul()))),
            Case("NTILE(n) OVER (ORDER BY a)", WindowFunctionType.Ntile(v("n"))),
            // FIRST_VALUE
            Case("FIRST_VALUE(x) OVER (ORDER BY a)", WindowFunctionType.FirstValue(v("x"), null)),
            Case("FIRST_VALUE(x) RESPECT NULLS OVER (ORDER BY a)", WindowFunctionType.FirstValue(v("x"), RESPECT)),
            Case("first_value(x) ignore nulls OVER (PARTITION BY p)", WindowFunctionType.FirstValue(v("x"), IGNORE)),
            // LAST_VALUE
            Case("LAST_VALUE(x) OVER w", WindowFunctionType.LastValue(v("x"), null)),
            Case("LAST_VALUE(x) RESPECT NULLS OVER w", WindowFunctionType.LastValue(v("x"), RESPECT)),
            Case("LAST_VALUE(x) IGNORE NULLS OVER (ORDER BY a)", WindowFunctionType.LastValue(v("x"), IGNORE)),
            // NTH_VALUE
            Case("NTH_VALUE(x, 2) OVER (ORDER BY a)", WindowFunctionType.NthValue(v("x"), int(2), null, null)),
            Case("NTH_VALUE(x, 2) FROM FIRST OVER (ORDER BY a)", WindowFunctionType.NthValue(v("x"), int(2), FROM_FIRST, null)),
            Case("NTH_VALUE(x, 2) FROM LAST OVER (ORDER BY a)", WindowFunctionType.NthValue(v("x"), int(2), FROM_LAST, null)),
            Case("NTH_VALUE(x, 2) IGNORE NULLS OVER (ORDER BY a)", WindowFunctionType.NthValue(v("x"), int(2), null, IGNORE)),
            Case("NTH_VALUE(x, 2) FROM FIRST RESPECT NULLS OVER w", WindowFunctionType.NthValue(v("x"), int(2), FROM_FIRST, RESPECT)),
            Case("nth_value(x, 3) from last ignore nulls OVER w", WindowFunctionType.NthValue(v("x"), int(3), FROM_LAST, IGNORE)),
        )
    }
}
