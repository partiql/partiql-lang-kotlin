/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import org.junit.Test

class EvaluatorTest : Base() {
    val evaluator = Evaluator(ion)

    fun eval(source: String): ExprValue =
        evaluator
            .compile(source)
            .eval(
                Bindings.over {
                    when (it) {
                        "i" -> literal("1").exprValue()
                        "f" -> literal("2e0").exprValue()
                        "d" -> literal("3d0").exprValue()
                        "s" -> literal("\"hello\"").exprValue()
                        else -> null
                    }
                }
            )

    fun voidEval(source: String) { eval(source) }

    fun assertEval(source: String,
             expectedLit: String,
             block: AssertExprValue.() -> Unit = { }) {
        val expectedIon = literal(expectedLit)
        val exprVal = eval(source)
        AssertExprValue(exprVal)
            .apply {
                assertIonValue(expectedIon)
            }
            .run(block)
    }

    @Test(expected = IllegalArgumentException::class)
    fun empty() = voidEval("")

    @Test
    fun literal() = assertEval("5", "5")

    @Test
    fun identifier() = assertEval("i", "1")

    @Test
    fun functionCall() = assertEval("sexp(i, f, d)", "(1 2e0 3d0)")

    @Test
    fun listLiteral() = assertEval("[i, f, d]", "[1, 2e0, 3d0]")

    @Test
    fun structLiteral() = assertEval("{a:i, b:f, c:d}", "{a:1, b:2e0, c:3d0}")

    @Test
    fun unaryPlus() = assertEval("+i", "1")

    @Test
    fun unaryMinus() = assertEval("-f", "-2e0")

    @Test
    fun addIntFloat() = assertEval("i + f", "3e0")
}