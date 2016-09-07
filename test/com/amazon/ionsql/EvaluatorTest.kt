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
    fun emptyThrows() = voidEval("")

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

    @Test
    fun subIntFloatDecimal() = assertEval("i - f - d", "-4.0")

    @Test
    fun mulFloatIntInt() = assertEval("f * 2 * 4", "16e0")

    @Test
    fun divDecimalInt() = assertEval("d / 2", "1.5")

    @Test
    fun modIntInt() = assertEval("3 % 2", "1")

    @Test
    fun moreIntFloat() = assertEval("3 > 2e0", "true")

    @Test
    fun moreIntFloatFalse() = assertEval("1 > 2e0", "false")

    @Test
    fun lessIntFloat() = assertEval("1 < 2e0", "true")

    @Test
    fun lessIntFloatFalse() = assertEval("3 < 2e0", "false")

    @Test
    fun moreEqIntFloat() = assertEval("3 >= 2e0", "true")

    @Test
    fun moreEqIntFloatFalse() = assertEval("1 >= 2e0", "false")

    @Test
    fun lessEqIntFloat() = assertEval("1 <= 2e0", "true")

    @Test
    fun lessEqIntFloatFalse() = assertEval("5 <= 2e0", "false")

    @Test
    fun equalIntFloat() = assertEval("1 == 1e0", "true")

    @Test
    fun equalIntFloatFalse() = assertEval("1 == 1e1", "false")

    @Test
    fun notEqualIntFloat() = assertEval("1 != 2e0", "true")

    @Test
    fun notEqualIntFloatFalse() = assertEval("1 != 1e0", "false")

    @Test(expected = IllegalArgumentException::class)
    fun notOnNonBooleanThrows() = voidEval("!i")

    @Test
    fun notTrue() = assertEval("not true", "false")

    @Test
    fun notFalse() = assertEval("not false", "true")

    @Test
    fun andTrueFalse() = assertEval("true and false", "false")

    @Test
    fun andTrueTrue() = assertEval("true and true", "true")

    @Test
    fun orTrueFalse() = assertEval("true or false", "true")

    @Test
    fun orFalseFalse() = assertEval("false or false", "false")

    @Test
    fun comparisonsConjuctTrue() = assertEval(
        "i < f and f < d",
        "true"
    )

    @Test
    fun comparisonsDisjunctFalse() = assertEval(
        "i < f and (f > d or i > d)",
        "false"
    )
}