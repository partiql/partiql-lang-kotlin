/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import org.junit.Before
import org.junit.Test

class InfixParserTest : Base() {
    val parser = InfixParser(ion)
    val tokenizer = Tokenizer(ion)

    fun assertExpression(expectedText: String, source: String) {
        val tokens = tokenizer.tokenize(literal("($source)"))
        val actual = parser.parse(tokens)
        val expected = literal(expectedText)

        assertEquals(expected, actual)
    }

    @Test
    fun lit() = assertExpression(
        "(lit 5)",
        "5"
    )

    @Test
    fun id() = assertExpression(
        "(id kumo)",
        "kumo"
    )

    @Test
    fun callEmpty() = assertExpression(
        "(call foobar)",
        "foobar()"
    )

    @Test
    fun callWithMultiple() = assertExpression(
        "(call foobar (lit 5) (lit 6) (id a))",
        "foobar(5, 6, a)"
    )

    @Test
    fun selectWithSingleFrom() = assertExpression(
        "(select ((id a)) (from (id table)))",
        "SELECT a FROM table"
    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhere() = assertExpression(
        """(select
             ((id a) (id b))
             (from (as t1 (id table1)) (id table2))
             (where (call f (id t1)))
           )
        """,
        "SELECT a, b FROM table1 as t1, table2 WHERE f(t1)"
    )

    @Test
    fun dot() = assertExpression(
        "(. (id a) (id b))",
        "a.b"
    )

    @Test
    fun dotStar() = assertExpression(
        "(. (call foo (id x) (id y)) (id a) (*) (id b))",
        "foo(x, y).a.*.b"
    )

    @Test
    fun dotDot() = assertExpression(
        "(. (call foo (id x) (id y)) (..) (..) (..) (id a))",
        "foo(x, y)....a"
    )

    @Test
    fun dotDotAndStar() = assertExpression(
        "(. (id x) (..) (..) (..) (id a) (..) (*) (id b))",
        "x....a..*.b"
    )
}