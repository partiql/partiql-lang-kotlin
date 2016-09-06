/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSexp
import org.junit.Before
import org.junit.Test

class ParserTest : Base() {
    val parser = Parser(ion)
    val tokenizer = Tokenizer(ion)

    fun parse(source: String): IonSexp {
        val tokens = tokenizer.tokenize(literal("($source)"))
        val ast = parser.parse(tokens)
        return ast
    }

    fun assertExpression(expectedText: String, source: String) {
        val actual = parse(source)
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
    fun listLiteral() = assertExpression(
        "(list (id a) (lit 5) (+ (id b) (lit 6)))",
        "[a, 5, (b + 6)]"
    )

    @Test
    fun structLiteral() = assertExpression(
        """(struct
             (list (lit "x") (id a))
             (list (lit "y") (lit 5))
             (list (lit "z") (+ (id b) (lit 6)))
           )
        """,
        "{x:a, y:5, z:(b + 6)}"
    )

    @Test
    fun callEmpty() = assertExpression(
        "(call foobar)",
        "foobar()"
    )

    @Test
    fun unaryMinusCall() = assertExpression(
        "(- (call baz))",
        "-baz()"
    )

    @Test
    fun unaryPlusMinusIdent() = assertExpression(
        "(+ (- (call baz)))",
        "+(-baz())"
    )

    @Test
    fun unaryPlusMinusIdentNoSpaces() = assertExpression(
        "(+ (- (call baz)))",
        "+-baz()"
    )

    @Test
    fun binaryOperatorsWithPrecedence() = assertExpression(
        """(and
             (+
               (id a)
               (id b)
             )
             (-
               (*
                 (/ (id c) (id d))
                 (id e)
               )
               (id f)
             )
           )
        """,
        "a + b and c / d * e - f"
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
    fun selectStar() = assertExpression(
        "(select () (from (id table)))",
        "SELECT * FROM table"
    )

    @Test(expected = IllegalArgumentException::class)
    fun selectNothing() {
        parse("SELECT FROM table")
    }

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
        """(. (id a) (lit "b"))""",
        "a.b"
    )

    @Test
    fun dotStar() = assertExpression(
        """(. (call foo (id x) (id y)) (lit "a") (*) (lit "b"))""",
        "foo(x, y).a.*.b"
    )

    @Test
    fun dotDot() = assertExpression(
        """(. (call foo (id x) (id y)) (..) (..) (..) (lit "a"))""",
        "foo(x, y)....a"
    )

    @Test
    fun dotDotAndStar() = assertExpression(
        """(. (id x) (..) (..) (..) (lit "a") (..) (*) (lit "b"))""",
        "x....a..*.b"
    )

    @Test
    fun bracket() = assertExpression(
        """(. (id a) (lit 5) (lit "b") (+ (id a) (lit 3)))""",
        """a[5]["b"][(a + 3)]"""
    )

    @Test
    fun pathsAndSelect() = assertExpression(
        """(select
             (
               (as a (. (call process (id t1)) (..) (lit "a") (lit 0)))
               (as b (. (id t2) (lit "b")))
             )
             (from
               (id t1)
               (. (id t2) (lit "x") (*) (lit "b"))
             )
             (where
               (and
                 (call test (. (id t2) (..) (..) (lit "name")) (. (id t1) (lit "name")))
                 (== (. (id t1) (lit "id")) (. (id t2) (lit "id")))
               )
             )
           )
        """,
        """SELECT process(t1)..a[0] AS a, t2.b AS b
           FROM t1, t2.x.*.b
           WHERE test(t2...name, t1.name) AND t1.id == t2.id
        """
    )
}