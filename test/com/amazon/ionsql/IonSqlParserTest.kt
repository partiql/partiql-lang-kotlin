/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSexp
import org.junit.Test

class IonSqlParserTest : Base() {
    val parser = IonSqlParser(ion)

    fun parse(source: String): IonSexp = parser.parse(source)

    fun assertExpression(expectedText: String, source: String) {
        val actual = parse(source).filterMetaNodes()
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
             (lit "x") (id a)
             (lit "y") (lit 5)
             (lit "z") (+ (id b) (lit 6))
           )
        """,
        "{'x':a, 'y':5, 'z':(b + 6)}"
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
             (||
               (-
                 (*
                   (/ (id c) (id d))
                   (id e)
                 )
                 (id f)
               )
               (id g)
             )
           )
        """,
        "a + b and c / d * e - f || g"
    )

    @Test
    fun betweenOperator() = assertExpression(
        """(between (lit 5) (lit 1) (lit 10))""",
        "5 BETWEEN 1 AND 10"
    )

    @Test
    fun callWithMultiple() = assertExpression(
        "(call foobar (lit 5) (lit 6) (id a))",
        "foobar(5, 6, a)"
    )

    @Test
    fun selectWithSingleFrom() = assertExpression(
        "(select (list (id a)) (from (id table1)))",
        "SELECT a FROM table1"
    )

    @Test
    fun selectAllWithSingleFrom() = assertExpression(
        "(select (list (id a)) (from (id table1)))",
        "SELECT ALL a FROM table1"
    )

    @Test
    fun selectDistinctWithSingleFrom() = assertExpression(
        "(select (distinct (list (id a))) (from (id table1)))",
        "SELECT DISTINCT a FROM table1"
    )

    @Test
    fun selectStar() = assertExpression(
        "(select (*) (from (id table1)))",
        "SELECT * FROM table1"
    )

    @Test
    fun selectAllStar() = assertExpression(
        "(select (*) (from (id table1)))",
        "SELECT ALL * FROM table1"
    )

    @Test
    fun selectDistinctStar() = assertExpression(
        "(select (distinct (*)) (from (id table1)))",
        "SELECT DISTINCT * FROM table1"
    )

    @Test
    fun selectValues() = assertExpression(
        "(select (values (id v)) (from (as v (id table1))))",
        "SELECT VALUES v FROM table1 AS v"
    )

    @Test
    fun selectAllValues() = assertExpression(
        "(select (values (id v)) (from (as v (id table1))))",
        "SELECT ALL VALUES v FROM table1 AS v"
    )

    @Test
    fun selectDistinctValues() = assertExpression(
        "(select (distinct (values (id v))) (from (as v (id table1))))",
        "SELECT DISTINCT VALUES v FROM table1 AS v"
    )

    @Test
    fun selectWithMissing() = assertExpression(
        "(select (list (id a)) (from (id stuff)) (where (is (id b) (missing))))",
        "SELECT a FROM stuff WHERE b IS MISSING"
    )

    @Test(expected = IllegalArgumentException::class)
    fun selectNothing() {
        parse("SELECT FROM table1")
    }

    @Test
    fun selectMultipleWithMultipleFromSimpleWhere() = assertExpression(
        """(select
             (list (id a) (id b))
             (from (as t1 (id table1)) (id table2))
             (where (call f (id t1)))
           )
        """,
        "SELECT a, b FROM table1 as t1, table2 WHERE f(t1)"
    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhereNoAsAlias() = assertExpression(
        """(select
             (list (as a1 (id a)) (as b1 (id b)))
             (from (as t1 (id table1)) (id table2))
             (where (call f (id t1)))
           )
        """,
        "SELECT a a1, b b1 FROM table1 t1, table2 WHERE f(t1)"
    )

    @Test
    fun selectCorrelatedJoin() = assertExpression(
        """(select
             (list (id a) (id b))
             (from (as s (id stuff)) (@ (id s)))
             (where (call f (id s)))
           )
        """,
        "SELECT a, b FROM stuff s, @s WHERE f(s)"
    )

    @Test
    fun dot() = assertExpression(
        """(path (id a) (lit "b"))""",
        "a.b"
    )

    @Test
    fun groupDot() = assertExpression(
        """(path (id a) (lit "b"))""",
        "(a).b"
    )

    @Test
    fun dotStar() = assertExpression(
        """(path (call foo (id x) (id y)) (lit "a") (*) (lit "b"))""",
        "foo(x, y).a.*.b"
    )

    @Test
    fun dotDotAndStar() = assertExpression(
        """(path (id x) (lit "a") (*) (lit "b"))""",
        "x.a.*.b"
    )

    @Test
    fun bracket() = assertExpression(
        """(path (id a) (lit 5) (lit "b") (+ (id a) (lit 3)))""",
        """a[5]['b'][(a + 3)]"""
    )

    @Test
    fun pathsAndSelect() = assertExpression(
        """(select
             (list
               (as a (path (call process (id t)) (lit "a") (lit 0)))
               (as b (path (id t2) (lit "b")))
             )
             (from
               (as t (path (id t1) (*) (lit "a")))
               (path (id t2) (*) (lit "x") (*) (lit "b"))
             )
             (where
               (and
                 (call test (path (id t2) (lit "name")) (path (id t1) (lit "name")))
                 (= (path (id t1) (lit "id")) (path (id t2) (lit "id")))
               )
             )
           )
        """,
        """SELECT process(t).a[0] AS a, t2.b AS b
           FROM t1.a AS t, t2.x.*.b
           WHERE test(t2.name, t1.name) AND t1.id = t2.id
        """
    )

    @Test
    fun nestedSelectNoWhere() = assertExpression(
        """(select
             (*)
             (from
               (path
                 (select
                   (*)
                   (from (id x))
                 )
                 (*)
                 (lit "a")
               )
             )
           )
        """,
        "SELECT * FROM (SELECT * FROM x).a"
    )

    @Test
    fun nestedSelect() = assertExpression(
        """(select
             (*)
             (from
               (path
                 (select
                   (*)
                   (from (id x))
                   (where (id b))
                 )
                 (*)
                 (lit "a")
               )
             )
           )
        """,
        "SELECT * FROM (SELECT * FROM x WHERE b).a"
    )

    @Test
    fun selectLimit() = assertExpression(
        """(select
             (*)
             (from (id a))
             (limit (lit 10))
           )
        """,
        "SELECT * FROM a LIMIT 10"
    )

    @Test
    fun selectWhereLimit() = assertExpression(
        """(select
             (*)
             (from (id a))
             (where (= (id a) (lit 5)))
             (limit (lit 10))
           )
        """,
        "SELECT * FROM a WHERE a = 5 LIMIT 10"
    )

    @Test
    fun castNoArgs() = assertExpression(
        """(cast
             (lit 5)
             (type character_varying)
           )
        """,
        "CAST(5 AS VARCHAR)"
    )

    @Test
    fun castArg() = assertExpression(
        """(cast
             (+ (lit 5) (id a))
             (type character_varying 1)
           )
        """,
        "CAST(5 + a AS VARCHAR(1))"
    )

    @Test(expected = IllegalArgumentException::class)
    fun castTooManyArgs() {
        parse("CAST(5 AS INTEGER(10))")
    }

    @Test(expected = IllegalArgumentException::class)
    fun castNonLiteralArg() {
        parse("CAST(5 AS VARCHAR(a))")
    }

    @Test(expected = IllegalArgumentException::class)
    fun castNegativeArg() {
        parse("CAST(5 AS VARCHAR(-1))")
    }

    @Test(expected = IllegalArgumentException::class)
    fun castNonTypArg() {
        parse("CAST(5 AS SELECT)")
    }
}
