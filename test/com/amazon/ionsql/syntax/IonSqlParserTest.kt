/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.syntax

import com.amazon.ion.*
import com.amazon.ionsql.*
import com.amazon.ionsql.util.*
import org.junit.*

class IonSqlParserTest : Base() {
    private val parser = IonSqlParser(ion)

    private fun parse(source: String): IonSexp = parser.parse(source)


    private fun assertExpression(expectedText: String, source: String) {
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
    fun bagLiteral() = assertExpression(
        "(bag (id a) (lit 5) (+ (id b) (lit 6)))",
        "<<a, 5, (b + 6)>>"
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
    fun nestedEmptyListLiteral() = assertExpression(
        "(list (list))",
        "[[]]"
    )

    @Test
    fun nestedEmptyBagLiteral() = assertExpression(
        "(bag (bag))",
        "<<<<>>>>"
    )

    @Test
    fun nestedEmptyStructLiteral() = assertExpression(
        """(struct (lit "a") (struct))""",
        "{'a':{}}"
    )

    @Test
    fun callEmpty() = assertExpression(
            "(call foobar)",
            "foobar()"
    )

    @Test
    fun callOneArgument() = assertExpression(
            "(call foobar (lit 1))",
            "foobar(1)"
    )

    @Test
    fun callTwoArgument() = assertExpression(
            "(call foobar (lit 1) (lit 2))",
            "foobar(1, 2)"
    )

    @Test
    fun callSubstringSql92Syntax() = assertExpression(
            "(call substring (lit \"test\") (lit 100))",
            "substring('test' from 100)"
    )

    @Test
    fun callSubstringSql92SyntaxWithLength() = assertExpression(
            "(call substring (lit \"test\") (lit 100) (lit 50))",
            "substring('test' from 100 for 50)"
    )

    @Test
    fun callSubstringNormalSyntax() = assertExpression(
            "(call substring (lit \"test\") (lit 100))",
            "substring('test', 100)"
    )

    @Test
    fun callSubstringNormalSyntaxWithLength() = assertExpression(
            "(call substring (lit \"test\") (lit 100) (lit 50))",
            "substring('test', 100, 50)"
    )

    @Test(expected = ParserException::class)
    fun callTrimZeroArguments() {
         parse("trim()")
    }

    @Test
    fun callTrimSingleArgument() = assertExpression("(call trim (lit \"test\"))",
                                                    "trim('test')")

    @Test
    fun callTrimSingleArgumentWithFrom() = assertExpression("(call trim (lit \"test\"))",
                                                            "trim(from 'test')")

    @Test
    fun callTrimTwoArgumentsUsingBoth() = assertExpression("(call trim (lit \"both\") (lit \"test\"))",
                                                           "trim(both from 'test')")

    @Test
    fun callTrimTwoArgumentsUsingLeading() = assertExpression("(call trim (lit \"leading\") (lit \"test\"))",
                                                              "trim(leading from 'test')")

    @Test
    fun callTrimTwoArgumentsUsingTrailing() = assertExpression("(call trim (lit \"trailing\") (lit \"test\"))",
                                                               "trim(trailing from 'test')")


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
    fun mixedAddMinusAndUnaryPlusMinus() = assertExpression(
        """(and
             (+
               (id a)
               (lit -5.)
             )
             (-
               (id c)
               (lit 7.0)
             )
           )
        """,
        "(a+-5e0) and (c-+7.0)"
    )

    @Test
    fun mixedMultiplyComparisonAndUnaryPlusMinus() = assertExpression(
        """(and
             (*
               (id d)
               (lit 9)
             )
             (>=
               (id e)
               (+ (- (+ (id foo))))
             )
           )
        """,
        "d*-+-9 and e>=+-+foo"
    )

    @Test
    fun unaryIonFloatLiteral() = assertExpression(
        """
        (lit 5e0)
        """,
        "+-+-+-`-5e0`"
    )

    @Test
    fun unaryIonTimestampLiteral() = assertExpression(
        """
        (+ (- (lit 2017-01-01T)))
        """,
        "+-`2017-01-01`"
    )

    @Test
    fun betweenOperator() = assertExpression(
        """(between (lit 5) (lit 1) (lit 10))""",
        "5 BETWEEN 1 AND 10"
    )

    @Test
    fun notBetweenOperator() = assertExpression(
        """(not_between (lit 5) (lit 1) (lit 10))""",
        "5 NOT BETWEEN 1 AND 10"
    )

    @Test
    fun atOperatorOnIdentifier() = assertExpression(
        """(@ (id a))""",
        "@a"
    )

    @Test
    fun atOperatorOnPath() = assertExpression(
        """(path (@ (id a)) (lit "b"))""",
        "@a.b"
    )

    @Test(expected = ParserException::class)
    fun atOperatorOnNonIdentifier() {
        parse("@(a)")
    }

    @Test(expected = ParserException::class)
    fun atOperatorDoubleOnIdentifier() {
        parse("@ @a")
    }

    @Test
    fun nullIsNull() = assertExpression(
        "(is (lit null) (type 'null'))",
        "null IS NULL"
    )

    @Test
    fun missingIsMissing() = assertExpression(
        "(is (missing) (type missing))",
        "mIsSiNg IS MISSING"
    )

    @Test
    fun callIsVarchar() = assertExpression(
        "(is (call f) (type character_varying 200))",
        "f() IS VARCHAR(200)"
    )

    @Test(expected = ParserException::class)
    fun nullIsNullIonLiteral() {
        parse("NULL is `null`")
    }

    @Test(expected = ParserException::class)
    fun idIsStringLiteral() {
        parse("a is 'missing'")
    }

    @Test(expected = ParserException::class)
    fun idIsGroupMissing() {
        parse("a is (missing)")
    }

    @Test
    fun nullIsNotNull() = assertExpression(
        "(is_not (lit null) (type 'null'))",
        "null IS NOT NULL"
    )

    @Test
    fun missingIsNotMissing() = assertExpression(
        "(is_not (missing) (type missing))",
        "mIsSiNg IS NOT MISSING"
    )

    @Test
    fun callIsNotVarchar() = assertExpression(
        "(is_not (call f) (type character_varying 200))",
        "f() IS NOT VARCHAR(200)"
    )

    @Test(expected = ParserException::class)
    fun nullIsNotNullIonLiteral() {
        parse("NULL is not `null`")
    }

    @Test(expected = ParserException::class)
    fun idIsNotStringLiteral() {
        parse("a is not 'missing'")
    }

    @Test(expected = ParserException::class)
    fun idIsNotGroupMissing() {
        parse("a is not (missing)")
    }

    @Test
    fun callWithMultiple() = assertExpression(
        "(call foobar (lit 5) (lit 6) (id a))",
        "foobar(5, 6, a)"
    )

    @Test
    fun selectWithSingleFrom() = assertExpression(
        "(select (project (list (id a))) (from (id table1)))",
        "SELECT a FROM table1"
    )

    @Test
    fun selectAllWithSingleFrom() = assertExpression(
        "(select (project (list (id a))) (from (id table1)))",
        "SELECT ALL a FROM table1"
    )

    @Test
    fun selectDistinctWithSingleFrom() = assertExpression(
        "(select (project_distinct (list (id a))) (from (id table1)))",
        "SELECT DISTINCT a FROM table1"
    )

    @Test
    fun selectStar() = assertExpression(
        "(select (project (*)) (from (id table1)))",
        "SELECT * FROM table1"
    )

    @Test
    fun selectWithFromAt() = assertExpression(
        "(select (project (list (id ord))) (from (at ord (id table1))))",
        "SELECT ord FROM table1 AT ord"
    )

    @Test
    fun selectWithFromAsAndAt() = assertExpression(
        "(select (project (list (id ord) (id val))) (from (at ord (as val (id table1)))))",
        "SELECT ord, val FROM table1 AS val AT ord"
    )

    @Test(expected = ParserException::class)
    fun selectWithFromAtAndAs() {
        parse("SELECT ord, val FROM table1 AT ord AS val")
    }

    @Test
    fun selectWithFromUnpivot() = assertExpression(
        """
        (select
          (project (*))
          (from (unpivot (id item)))
        )
        """,
        "SELECT * FROM UNPIVOT item"
    )

    @Test
    fun selectWithFromUnpivotWithAt() = assertExpression(
        """
        (select
          (project (list (id ord)))
          (from (at name (unpivot (id item))))
        )
        """,
        "SELECT ord FROM UNPIVOT item AT name"
    )

    @Test
    fun selectWithFromUnpivotWithAs() = assertExpression(
        """
        (select
          (project (list (id ord)))
          (from (as val (unpivot (id item))))
        )
        """,
        "SELECT ord FROM UNPIVOT item AS val"
    )

    @Test
    fun selectWithFromUnpivotWithAsAndAt() = assertExpression(
        """
        (select
          (project (list (id ord)))
          (from (at name (as val (unpivot (id item)))))
        )
        """,
        "SELECT ord FROM UNPIVOT item AS val AT name"
    )

    @Test
    fun selectAllStar() = assertExpression(
        "(select (project (*)) (from (id table1)))",
        "SELECT ALL * FROM table1"
    )

    @Test
    fun selectDistinctStar() = assertExpression(
        "(select (project_distinct (*)) (from (id table1)))",
        "SELECT DISTINCT * FROM table1"
    )

    @Test
    fun selectValues() = assertExpression(
        "(select (project (value (id v))) (from (as v (id table1))))",
        "SELECT VALUE v FROM table1 AS v"
    )

    @Test
    fun selectAllValues() = assertExpression(
        "(select (project (value (id v))) (from (as v (id table1))))",
        "SELECT ALL VALUE v FROM table1 AS v"
    )

    @Test
    fun selectDistinctValues() = assertExpression(
        "(select (project_distinct (value (id v))) (from (as v (id table1))))",
        "SELECT DISTINCT VALUE v FROM table1 AS v"
    )

    @Test
    fun selectWithMissing() = assertExpression(
        "(select (project (list (id a))) (from (id stuff)) (where (is (id b) (type missing))))",
        "SELECT a FROM stuff WHERE b IS MISSING"
    )

    @Test(expected = ParserException::class)
    fun selectNothing() {
        parse("SELECT FROM table1")
    }

    @Test
    fun selectMultipleWithMultipleFromSimpleWhere() = assertExpression(
        """(select
             (project (list (id a) (id b)))
             (from (inner_join (as t1 (id table1)) (id table2)))
             (where (call f (id t1)))
           )
        """,
        "SELECT a, b FROM table1 as t1, table2 WHERE f(t1)"
    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhereNoAsAlias() = assertExpression(
        """(select
             (project (list (as a1 (id a)) (as b1 (id b))))
             (from (inner_join (as t1 (id table1)) (id table2)))
             (where (call f (id t1)))
           )
        """,
        "SELECT a a1, b b1 FROM table1 t1, table2 WHERE f(t1)"
    )

    @Test
    fun selectCorrelatedJoin() = assertExpression(
        """(select
             (project (list (id a) (id b)))
             (from (inner_join (as s (id stuff)) (@ (id s))))
             (where (call f (id s)))
           )
        """,
        "SELECT a, b FROM stuff s, @s WHERE f(s)"
    )

    @Test
    fun selectCorrelatedExplicitInnerJoin() = assertExpression(
        """(select
             (project (list (id a) (id b)))
             (from (inner_join (as s (id stuff)) (@ (id s))))
             (where (call f (id s)))
           )
        """,
        "SELECT a, b FROM stuff s INNER JOIN @s WHERE f(s)"
    )

    @Test
    fun selectCorrelatedExplicitCrossJoin() = assertExpression(
        """(select
             (project (list (id a) (id b)))
             (from (inner_join (as s (id stuff)) (@ (id s))))
             (where (call f (id s)))
           )
        """,
        "SELECT a, b FROM stuff s CROSS JOIN @s WHERE f(s)"
    )

    @Test
    fun selectCorrelatedExplicitLeftJoin() = assertExpression(
        """(select
             (project (list (id a) (id b)))
             (from (left_join (as s (id stuff)) (@ (id s))))
             (where (call f (id s)))
           )
        """,
        "SELECT a, b FROM stuff s LEFT JOIN @s WHERE f(s)"
    )

    @Test
    fun selectCorrelatedLeftOuterJoinOn() = assertExpression(
        """(select
             (project (list (id a) (id b)))
             (from
               (left_join
                 (as s (id stuff))
                 (@ (id s))
                 (call f (id s))
               )
             )
           )
        """,
        "SELECT a, b FROM stuff s LEFT JOIN @s ON f(s)"
    )

    @Test
    fun selectRightJoin() = assertExpression(
        """(select
             (project (list (id a) (id b)))
             (from
               (right_join
                 (as s (id stuff))
                 (as f (id foo))
               )
             )
           )
        """,
        "SELECT a, b FROM stuff s RIGHT JOIN foo f"
    )

    @Test
    fun selectFullOuterJoinOn() = assertExpression(
        """(select
             (project (list (id a) (id b)))
             (from
               (outer_join
                 (as s (id stuff))
                 (as f (id foo))
                 (= (id s) (id f))
               )
             )
           )
        """,
        "SELECT a, b FROM stuff s FULL OUTER JOIN foo f ON s = f"
    )

    @Test
    fun selectJoins() = assertExpression(
        """(select
             (project (list (id x)))
             (from
               (outer_join
                 (right_join
                   (left_join
                     (inner_join
                       (inner_join
                         (id a)
                         (id b)
                       )
                       (id c)
                     )
                     (id d)
                     (id e)
                   )
                   (id f)
                 )
                 (id g)
                 (id h)
               )
             )
           )
        """,
        "SELECT x FROM a, b CROSS JOIN c LEFT JOIN d ON e RIGHT OUTER JOIN f OUTER JOIN g ON h"
    )

    @Test
    fun aggregateFunctionCall() = assertExpression(
        """(call_agg count all (id a))""",
        "COUNT(a)"
    )

    @Test
    fun selectListWithAggregateWildcardCall() = assertExpression(
        """
        (select
          (project
            (list
              (+ (call_agg sum all (id a)) (call_agg_wildcard count))
              (call_agg avg all (id b))
              (call_agg min all (id c))
              (call_agg max all (+ (id d) (id e)))
            )
          )
          (from (id foo))
        )
        """,
        "SELECT sum(a) + count(*), AVG(b), MIN(c), MAX(d + e) FROM foo"
    )

    @Test(expected = ParserException::class)
    fun aggregateWithNoArgs() {
        parse("SUM()")
    }

    @Test(expected = ParserException::class)
    fun aggregateWithTooManyArgs() {
        parse("SUM(a, b)")
    }

    @Test(expected = ParserException::class)
    fun aggregateWithWildcardOnNonCount() {
        parse("SUM(*)")
    }

    @Test(expected = ParserException::class)
    fun aggregateWithWildcardOnNonCountNonAggregate() {
        parse("F(*)")
    }

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
        """(path (call foo (id x) (id y)) (lit "a") (* unpivot) (lit "b"))""",
        "foo(x, y).a.*.b"
    )

    @Test
    fun dotAndBracketStar() = assertExpression(
        """(path (id x) (lit "a") (*) (lit "b"))""",
        "x.a[*].b"
    )

    @Test(expected = ParserException::class)
    fun tooManyDots() {
        parse("x...a")
    }

    @Test
    fun bracket() = assertExpression(
        """(path (id a) (lit 5) (lit "b") (+ (id a) (lit 3)))""",
        """a[5]['b'][(a + 3)]"""
    )

    @Test
    fun pathsAndSelect() = assertExpression(
        """(select
             (project
               (list
                 (as a (path (call process (id t)) (lit "a") (lit 0)))
                 (as b (path (id t2) (lit "b")))
               )
             )
             (from
               (inner_join
                 (as t (path (id t1) (lit "a")))
                 (path (id t2) (lit "x") (* unpivot) (lit "b"))
               )
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
             (project (*))
             (from
               (path
                 (select
                   (project (*))
                   (from (id x))
                 )
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
             (project (*))
             (from
               (path
                 (select
                   (project (*))
                   (from (id x))
                   (where (id b))
                 )
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
             (project (*))
             (from (id a))
             (limit (lit 10))
           )
        """,
        "SELECT * FROM a LIMIT 10"
    )

    @Test
    fun selectWhereLimit() = assertExpression(
        """(select
             (project (*))
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

    @Test(expected = ParserException::class)
    fun castTooManyArgs() {
        parse("CAST(5 AS INTEGER(10))")
    }

    @Test(expected = ParserException::class)
    fun castNonLiteralArg() {
        parse("CAST(5 AS VARCHAR(a))")
    }

    @Test(expected = ParserException::class)
    fun castNegativeArg() {
        parse("CAST(5 AS VARCHAR(-1))")
    }

    @Test(expected = ParserException::class)
    fun castNonTypArg() {
        parse("CAST(5 AS SELECT)")
    }

    @Test
    fun searchedCaseSingleNoElse() = assertExpression(
        """(searched_case
             (when
               (= (id name) (lit "zoe"))
               (lit 1)
             )
           )
        """,
        "CASE WHEN name = 'zoe' THEN 1 END"
    )

    @Test
    fun searchedCaseSingleWithElse() = assertExpression(
        """(searched_case
             (when
               (= (id name) (lit "zoe"))
               (lit 1)
             )
             (else (lit 0))
           )
        """,
        "CASE WHEN name = 'zoe' THEN 1 ELSE 0 END"
    )

    @Test
    fun searchedCaseMultiWithElse() = assertExpression(
        """(searched_case
             (when
               (= (id name) (lit "zoe"))
               (lit 1)
             )
             (when
               (> (id name) (lit "kumo"))
               (lit 2)
             )
             (else (lit 0))
           )
        """,
        "CASE WHEN name = 'zoe' THEN 1 WHEN name > 'kumo' THEN 2 ELSE 0 END"
    )

    @Test
    fun simpleCaseSingleNoElse() = assertExpression(
        """(simple_case
             (id name)
             (when
               (lit "zoe")
               (lit 1)
             )
           )
        """,
        "CASE name WHEN 'zoe' THEN 1 END"
    )

    @Test
    fun simpleCaseSingleWithElse() = assertExpression(
        """(simple_case
             (id name)
             (when
               (lit "zoe")
               (lit 1)
             )
             (else (lit 0))
           )
        """,
        "CASE name WHEN 'zoe' THEN 1 ELSE 0 END"
    )

    @Test
    fun simpleCaseMultiWithElse() = assertExpression(
        """(simple_case
             (id name)
             (when
               (lit "zoe")
               (lit 1)
             )
             (when
               (lit "kumo")
               (lit 2)
             )
             (when
               (lit "mary")
               (lit 3)
             )
             (else (lit 0))
           )
        """,
        "CASE name WHEN 'zoe' THEN 1 WHEN 'kumo' THEN 2 WHEN 'mary' THEN 3 ELSE 0 END"
    )

    @Test(expected = ParserException::class)
    fun caseOnlyEnd() {
        parse("CASE END")
    }

    @Test(expected = ParserException::class)
    fun searchedCaseNoWhenWithElse() {
        parse("CASE ELSE 1 END")
    }

    @Test(expected = ParserException::class)
    fun simpleCaseNoWhenWithElse() {
        parse("CASE name ELSE 1 END")
    }

    @Test
    fun rowValueConstructorWithSimpleExpressions() = assertExpression(
        """(list (lit 1) (lit 2) (lit 3) (lit 4))""",
        "(1, 2, 3, 4)"
    )

    @Test
    fun rowValueConstructorWithRowValueConstructors() = assertExpression(
        """(list (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))""",
        "((1, 2), (3, 4))"
    )

    @Test
    fun tableValueConstructorWithRowValueConstructors() = assertExpression(
        """(bag (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))""",
        "VALUES (1, 2), (3, 4)"
    )

    @Test
    fun tableValueConstructorWithSingletonRowValueConstructors() = assertExpression(
        """(bag (list (lit 1)) (list (lit 2)) (list (lit 3)))""",
        "VALUES (1), (2), (3)"
    )

    @Test
    fun inOperatorWithImplicitValues() = assertExpression(
        """(in
             (id a)
             (list (lit 1) (lit 2) (lit 3) (lit 4))
           )
        """,
        "a IN (1, 2, 3, 4)"
    )

    @Test
    fun notInOperatorWithImplicitValues() = assertExpression(
        """(not_in
             (id a)
             (list (lit 1) (lit 2) (lit 3) (lit 4))
           )
        """,
        "a NOT IN (1, 2, 3, 4)"
    )

    @Test
    fun inOperatorWithImplicitValuesRowConstructor() = assertExpression(
        """(in
             (list (id a) (id b))
             (list (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))
           )
        """,
        "(a, b) IN ((1, 2), (3, 4))"
    )

    @Test
    fun groupBySingleId() = assertExpression(
        """(select
             (project (list (id a)))
             (from (id data))
             (group (by (id a)))
           )
        """,
        "SELECT a FROM data GROUP BY a"
    )

    @Test
    fun groupBySingleExpr() = assertExpression(
        """(select
             (project (list (+ (id a) (id b))))
             (from (id data))
             (group (by (+ (id a) (id b))))
           )
        """,
        "SELECT a + b FROM data GROUP BY a + b"
    )

    @Test
    fun groupPartialByMultiAliasedAndGroupAliased() = assertExpression(
        """(select
             (project (list (id g)))
             (from (id data))
             (group_partial
               (by
                 (as x (id a))
                 (as y (+ (id b) (id c)))
                 (as z (call foo (id d)))
               )
               (name g)
             )
           )
        """,
        "SELECT g FROM data GROUP PARTIAL BY a AS x, b + c AS y, foo(d) AS z GROUP AS g"
    )

    @Test(expected = ParserException::class)
    fun groupByOrdinal() {
        parse("SELECT a FROM data GROUP BY 1")
    }

    @Test(expected = ParserException::class)
    fun groupByOutOfBoundsOrdinal() {
        parse("SELECT a FROM data GROUP BY 2")
    }

    @Test(expected = ParserException::class)
    fun groupByBadOrdinal() {
        parse("SELECT a FROM data GROUP BY -1")
    }

    @Test(expected = ParserException::class)
    fun groupByStringConstantOrdinal() {
        parse("SELECT a FROM data GROUP BY 'a'")
    }

    @Test(expected = ParserException::class)
    fun leftOvers() {
        parse("5 5")
    }

    @Test
    fun havingMinimal() = assertExpression(
        """
          (select
            (project (list (id a)))
            (from (id data))
            (having (= (id a) (id b)))
          )
        """,
        "SELECT a FROM data HAVING a = b"
    )

    @Test
    fun havingWithWhere() = assertExpression(
        """
          (select
            (project (list (id a)))
            (from (id data))
            (where (= (id a) (id b)))
            (having (= (id c) (id d)))
          )
        """,
        "SELECT a FROM data WHERE a = b HAVING c = d"
    )

    @Test
    fun havingWithWhereAndGroupBy() = assertExpression(
        """
          (select
            (project (list (id g)))
            (from (id data))
            (where (= (id a) (id b)))
            (group (by (id c) (id d)) (name g))
            (having (> (id d) (lit 6)))
          )
        """,
        "SELECT g FROM data WHERE a = b GROUP BY c, d GROUP AS g HAVING d > 6"
    )

    @Test
    fun pivotWithOnlyFrom() = assertExpression(
        """
          (pivot
            (member (id n) (id v))
            (from (id data))
          )
        """,
        "PIVOT v AT n FROM data"
    )

    @Test
    fun pivotHavingWithWhereAndGroupBy() = assertExpression(
        """
          (pivot
            (member (|| (lit "prefix:") (id c)) (id g))
            (from (id data))
            (where (= (id a) (id b)))
            (group (by (id c) (id d)) (name g))
            (having (> (id d) (lit 6)))
          )
        """,
        "PIVOT g AT ('prefix:' || c) FROM data WHERE a = b GROUP BY c, d GROUP AS g HAVING d > 6"
    )

    @Test(expected = ParserException::class)
    fun pivotNoAt() {
        parse("PIVOT v FROM data")
    }


    /*
    From SQL92 https://www.contrib.andrew.cmu.edu/~shadow/sql/sql1992.txt
         <like predicate>   ::= <match value> [ NOT ] LIKE <pattern>
                                    [ ESCAPE <escape character> ]

         <match value>      ::= <character value expression>
         <pattern>          ::= <character value expression>
         <escape character> ::= <character value expression>
     */
    @Test
    fun likeColNameLikeString() = assertExpression(
        """
        (select (project (list (id a))) (from (id data)) (where (like (id a) (lit "_AAA%"))))
        """,
        "SELECT a FROM data WHERE a LIKE '_AAA%'"
    )

    @Test
    fun likeColNameLikeColName() = assertExpression(
        """
        (select (project (list (id a) (id b))) (from (id data)) (where (like (id a) (id b))))
        """,
        "SELECT a, b FROM data WHERE a LIKE b"
    )

    @Test
    fun likeColNameLikeColNameDot() = assertExpression(
        """
        (select (project (*)) (from (as a (id data))) (where (like (path (id a) (lit "name")) (path (id b) (lit "pattern")))))
        """,
        "SELECT * FROM data as a WHERE a.name LIKE b.pattern"
    )


    @Test
    fun likeColNameLikeColNamePqth() = assertExpression(
        """
        (select (project (*)) (from (as a (id data))) (where (like (path (id a) (lit "name")) (path (id b) (lit "pattern")))))
        """,
        "SELECT * FROM data as a WHERE a.name LIKE b.pattern"
    )
    @Test
    fun likeColNameLikeStringEscape() = assertExpression(
        """
        (select (project (list (id a))) (from (id data)) (where (like (id a) (lit "_AAA%") (lit "["))))
        """,
        "SELECT a FROM data WHERE a LIKE '_AAA%' ESCAPE '[' "
    )

    @Test
    fun notLikeColNameLikeString() = assertExpression(
        """
        (select (project (list (id a))) (from (id data)) (where (not_like (id a) (lit "_AAA%"))))
        """,
        "SELECT a FROM data WHERE a NOT LIKE '_AAA%'"
    )

    @Test
    fun likeColNameLikeColNameEscape() = assertExpression(
        """
        (select (project (list (id a) (id b))) (from (id data)) (where (like (id a) (id b) (lit "\\"))))
        """, //  escape \ inside a Kotlin/Java String
        "SELECT a, b FROM data WHERE a LIKE b ESCAPE '\\'" // escape \ inside a Kotlin/Java String
    )

    @Test
    fun likeColNameLikeColNameEscapeNonLit() = assertExpression(
        """
        (select (project (list (id a) (id b))) (from (id data)) (where (like (id a) (id b) (id c))))
        """, //  escape \ inside a Kotlin/Java String
        "SELECT a, b FROM data WHERE a LIKE b ESCAPE c"
    )

    @Test
    fun likeColNameLikeColNameEscapePath() = assertExpression(
        """
        (select (project (list (id a) (id b))) (from (as x (id data))) (where (like (id a) (id b) (path (id x) (lit "c")))))
        """, //  escape \ inside a Kotlin/Java String
        "SELECT a, b FROM data as x WHERE a LIKE b ESCAPE x.c"


    )
    /*
    From SQL92 Spec
     3) "M NOT LIKE P" is equivalent to "NOT (M LIKE P)".
     */
    @Test
    fun likeNotEquivalent() = assertExpression(
        "(select (project (list (id a) (id b))) (from (id data)) (where (not (like (id a) (id b)))))",
        "SELECT a, b FROM data WHERE NOT (a LIKE b)"
    )

    @Test
    fun likeNotEquivalentWithEscape() = assertExpression(
        "(select (project (list (id a) (id b))) (from (id data)) (where (not (like (id a) (id b) (lit \"[\")))))",
        "SELECT a, b FROM data WHERE NOT (a LIKE b ESCAPE '[')"
    )

    @Test(expected = ParserException::class)
    fun likeColNameLikeColNameEscapeTypo() {
        parse("SELECT a, b FROM data WHERE a LIKE b ECSAPE '\\'")
    }

    @Test(expected = ParserException::class)
    fun likeWrongOrderOfArgs() {
        parse("SELECT a, b FROM data WHERE LIKE a b")
    }

    @Test(expected = ParserException::class)
    fun likeMissingEscapeValue() {
        parse("SELECT a, b FROM data WHERE a LIKE b ESCAPE")
    }

    @Test(expected = ParserException::class)
    fun likeMissingPattern() {
        parse("SELECT a, b FROM data WHERE a LIKE")
    }

    @Test(expected = ParserException::class)
    fun likeEscapeIncorrectOrder() {
        parse("SELECT a, b FROM data WHERE ECSAPE '\\' a LIKE b ")
    }

    @Test(expected = ParserException::class)
    fun likeEscapeAsSecondArgument() {
        parse("SELECT a, b FROM data WHERE a LIKE ECSAPE '\\' b ")
    }

    @Test(expected = ParserException::class)
    fun likeEscapeNotIncorrectOrder() {
        parse("SELECT a, b FROM data WHERE NOT a LIKE b ECSAPE '\\'")
    }

}