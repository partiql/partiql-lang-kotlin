/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.syntax

import com.amazon.ionsql.util.*
import org.junit.*

class IonSqlParserTest : IonSqlParserBase() {

    @Test
    fun lit() = assertExpression(
        "(lit 5)",
        "5"
    )

    @Test
    fun id_case_insensitive() = assertExpression(
        "(id kumo case_insensitive)",
        "kumo"
    )

    @Test
    fun id_case_sensitive() = assertExpression(
        "(id kumo case_sensitive)",
        "\"kumo\""
    )

    @Test
    fun listLiteral() = assertExpression(
        "(list (id a case_insensitive) (lit 5) (+ (id b case_insensitive) (lit 6)))",
        "[a, 5, (b + 6)]"
    )

    @Test
    fun bagLiteral() = assertExpression(
        "(bag (id a case_insensitive) (lit 5) (+ (id b case_insensitive) (lit 6)))",
        "<<a, 5, (b + 6)>>"
    )

    @Test
    fun structLiteral() = assertExpression(
                """(struct
                     (lit "x") (id a case_insensitive)
                     (lit "y") (lit 5)
                     (lit "z") (+ (id b case_insensitive) (lit 6))
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

    @Test
    fun callTrimSingleArgument() = assertExpression("(call trim (lit \"test\"))",
                                                    "trim('test')")



    @Test
    fun callTrimTwoArgumentsDefaultSpecification() = assertExpression("(call trim (lit \" \") (lit \"test\"))",
                                                                      "trim(' ' from 'test')")

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
        """(@ (id a case_insensitive))""",
        "@a"
    )

    @Test
    fun atOperatorOnPath() = assertExpression(
        """(path (@ (id a case_insensitive)) (case_insensitive (lit "b")))""",
        "@a.b"
    )

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

    @Test
    fun callWithMultiple() = assertExpression(
        "(call foobar (lit 5) (lit 6) (id a case_insensitive))",
        "foobar(5, 6, a)"
    )

    @Test
    fun selectWithSingleFrom() = assertExpression(
        "(select (project (list (id a case_insensitive))) (from (id table1 case_insensitive)))",
        "SELECT a FROM table1"
    )

    @Test
    fun selectAllWithSingleFrom() = assertExpression(
        "(select (project (list (id a case_insensitive))) (from (id table1 case_insensitive)))",
        "SELECT ALL a FROM table1"
    )

    @Test
    fun selectDistinctWithSingleFrom() = assertExpression(
        "(select (project_distinct (list (id a case_insensitive))) (from (id table1 case_insensitive)))",
        "SELECT DISTINCT a FROM table1"
    )

    @Test
    fun selectStar() = assertExpression(
        "(select (project (*)) (from (id table1 case_insensitive)))",
        "SELECT * FROM table1"
    )

    @Test
    fun selectWithFromAt() = assertExpression(
        "(select (project (list (id ord case_insensitive))) (from (at ord (id table1 case_insensitive))))",
        "SELECT ord FROM table1 AT ord"
    )

    @Test
    fun selectWithFromAsAndAt() = assertExpression(
        "(select (project (list (id ord case_insensitive) (id val case_insensitive))) (from (at ord (as val (id table1 case_insensitive)))))",
        "SELECT ord, val FROM table1 AS val AT ord"
    )

    @Test
    fun selectWithFromUnpivot() = assertExpression(
        """
        (select
          (project (*))
          (from (unpivot (id item case_insensitive)))
        )
        """,
        "SELECT * FROM UNPIVOT item"
    )

    @Test
    fun selectWithFromUnpivotWithAt() = assertExpression(
        """
        (select
          (project (list (id ord case_insensitive)))
          (from (at name (unpivot (id item case_insensitive))))
        )
        """,
        "SELECT ord FROM UNPIVOT item AT name"
    )

    @Test
    fun selectWithFromUnpivotWithAs() = assertExpression(
        """
        (select
          (project (list (id ord case_insensitive)))
          (from (as val (unpivot (id item case_insensitive))))
        )
        """,
        "SELECT ord FROM UNPIVOT item AS val"
    )

    @Test
    fun selectWithFromUnpivotWithAsAndAt() = assertExpression(
        """
        (select
          (project (list (id ord case_insensitive)))
          (from (at name (as val (unpivot (id item case_insensitive)))))
        )
        """,
        "SELECT ord FROM UNPIVOT item AS val AT name"
    )

    @Test
    fun selectAllStar() = assertExpression(
        "(select (project (*)) (from (id table1 case_insensitive)))",
        "SELECT ALL * FROM table1"
    )

    @Test
    fun selectDistinctStar() = assertExpression(
        "(select (project_distinct (*)) (from (id table1 case_insensitive)))",
        "SELECT DISTINCT * FROM table1"
    )

    @Test
    fun selectValues() = assertExpression(
        "(select (project (value (id v case_insensitive))) (from (as v (id table1 case_insensitive))))",
        "SELECT VALUE v FROM table1 AS v"
    )

    @Test
    fun selectAllValues() = assertExpression(
        "(select (project (value (id v case_insensitive))) (from (as v (id table1 case_insensitive))))",
        "SELECT ALL VALUE v FROM table1 AS v"
    )

    @Test
    fun selectDistinctValues() = assertExpression(
        "(select (project_distinct (value (id v case_insensitive))) (from (as v (id table1 case_insensitive))))",
        "SELECT DISTINCT VALUE v FROM table1 AS v"
    )

    @Test
    fun selectWithMissing() = assertExpression(
        "(select (project (list (id a case_insensitive))) (from (id stuff case_insensitive)) (where (is (id b case_insensitive) (type missing))))",
        "SELECT a FROM stuff WHERE b IS MISSING"
    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhere() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (inner_join (as t1 (id table1 case_insensitive)) (id table2 case_insensitive)))
             (where (call f (id t1 case_insensitive)))
           )
        """,
        "SELECT a, b FROM table1 as t1, table2 WHERE f(t1)"
    )

    @Test
    fun selectMultipleWithMultipleFromSimpleWhereNoAsAlias() = assertExpression(
        """(select
             (project (list (as a1 (id a case_insensitive)) (as b1 (id b case_insensitive))))
             (from (inner_join (as t1 (id table1 case_insensitive)) (id table2 case_insensitive)))
             (where (call f (id t1 case_insensitive)))
           )
        """,
        "SELECT a a1, b b1 FROM table1 t1, table2 WHERE f(t1)"
    )

    @Test
    fun selectCorrelatedJoin() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (inner_join (as s (id stuff case_insensitive)) (@ (id s case_insensitive))))
             (where (call f (id s case_insensitive)))
           )
        """,
        "SELECT a, b FROM stuff s, @s WHERE f(s)"
    )

    @Test
    fun selectCorrelatedExplicitInnerJoin() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (inner_join (as s (id stuff case_insensitive)) (@ (id s case_insensitive))))
             (where (call f (id s case_insensitive)))
           )
        """,
        "SELECT a, b FROM stuff s INNER JOIN @s WHERE f(s)"
    )

    @Test
    fun selectCorrelatedExplicitCrossJoin() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (inner_join (as s (id stuff case_insensitive)) (@ (id s case_insensitive))))
             (where (call f (id s case_insensitive)))
           )
        """,
        "SELECT a, b FROM stuff s CROSS JOIN @s WHERE f(s)"
    )

    @Test
    fun selectCorrelatedExplicitLeftJoin() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from (left_join (as s (id stuff case_insensitive)) (@ (id s case_insensitive))))
             (where (call f (id s case_insensitive)))
           )
        """,
        "SELECT a, b FROM stuff s LEFT JOIN @s WHERE f(s)"
    )

    @Test
    fun selectCorrelatedLeftOuterJoinOn() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from
               (left_join
                 (as s (id stuff case_insensitive))
                 (@ (id s case_insensitive))
                 (call f (id s case_insensitive))
               )
             )
           )
        """,
        "SELECT a, b FROM stuff s LEFT JOIN @s ON f(s)"
    )

    @Test
    fun selectRightJoin() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from
               (right_join
                 (as s (id stuff case_insensitive))
                 (as f (id foo case_insensitive))
               )
             )
           )
        """,
        "SELECT a, b FROM stuff s RIGHT JOIN foo f"
    )

    @Test
    fun selectFullOuterJoinOn() = assertExpression(
        """(select
             (project (list (id a case_insensitive) (id b case_insensitive)))
             (from
               (outer_join
                 (as s (id stuff case_insensitive))
                 (as f (id foo case_insensitive))
                 (= (id s case_insensitive) (id f case_insensitive))
               )
             )
           )
        """,
        "SELECT a, b FROM stuff s FULL OUTER JOIN foo f ON s = f"
    )

    @Test
    fun selectJoins() = assertExpression(
        """(select
             (project (list (id x case_insensitive)))
             (from
               (outer_join
                 (right_join
                   (left_join
                     (inner_join
                       (inner_join
                         (id a case_insensitive)
                         (id b case_insensitive)
                       )
                       (id c case_insensitive)
                     )
                     (id d case_insensitive)
                     (id e case_insensitive)
                   )
                   (id f case_insensitive)
                 )
                 (id g case_insensitive)
                 (id h case_insensitive)
               )
             )
           )
        """,
        "SELECT x FROM a, b CROSS JOIN c LEFT JOIN d ON e RIGHT OUTER JOIN f OUTER JOIN g ON h"
    )

    @Test
    fun aggregateFunctionCall() = assertExpression(
        """(call_agg count all (id a case_insensitive))""",
        "COUNT(a)"
    )

    @Test
    fun selectListWithAggregateWildcardCall() = assertExpression(
        """
        (select
          (project
            (list
              (+ (call_agg sum all (id a case_insensitive)) (call_agg_wildcard count))
              (call_agg avg all (id b case_insensitive))
              (call_agg min all (id c case_insensitive))
              (call_agg max all (+ (id d case_insensitive) (id e case_insensitive)))
            )
          )
          (from (id foo case_insensitive))
        )
        """,
        "SELECT sum(a) + count(*), AVG(b), MIN(c), MAX(d + e) FROM foo"
    )

    @Test
    fun dot_case_insensitive_component() = assertExpression(
        """(path (id a case_insensitive) (case_insensitive (lit "b")))""",
        "a.b"
    )

    @Test
    fun dot_case_sensitive() = assertExpression(
        """(path (id a case_sensitive) (case_sensitive (lit "b")))""",
        """ "a"."b" """
    )

    @Test
    fun dot_case_sensitive_component() = assertExpression(
        """(path (id a case_insensitive) (case_sensitive (lit "b")))""",
        "a.\"b\""
    )

    @Test
    fun groupDot() = assertExpression(
        """(path (id a case_insensitive) (case_insensitive (lit "b")))""",
        "(a).b"
    )

    @Test
    fun dotStar() = assertExpression(
        """(path (call foo (id x case_insensitive) (id y case_insensitive)) (case_insensitive (lit "a")) (* unpivot) (case_insensitive (lit "b")))""",
        "foo(x, y).a.*.b"
    )

    @Test
    fun dotAndBracketStar() = assertExpression(
        """(path (id x case_insensitive) (case_insensitive (lit "a")) (*) (case_insensitive (lit "b")))""",
        "x.a[*].b"
    )

    @Test
    fun bracket() = assertExpression(
        """(path (id a case_insensitive) (lit 5) (lit "b") (+ (id a case_insensitive) (lit 3)))""",
        """a[5]['b'][(a + 3)]"""
    )

    @Test
    fun pathsAndSelect() = assertExpression(
        """(select
             (project
               (list
                 (as a (path (call process (id t case_insensitive)) (case_insensitive (lit "a")) (lit 0)))
                 (as b (path (id t2 case_insensitive) (case_insensitive (lit "b"))))
               )
             )
             (from
               (inner_join
                 (as t (path (id t1 case_insensitive) (case_insensitive (lit "a"))))
                 (path (id t2 case_insensitive) (case_insensitive (lit "x")) (* unpivot) (case_insensitive (lit "b")))
               )
             )
             (where
               (and
                 (call test (path (id t2 case_insensitive) (case_insensitive (lit "name"))) (path (id t1 case_insensitive) (case_insensitive (lit "name"))))
                 (= (path (id t1 case_insensitive) (case_insensitive (lit "id"))) (path (id t2 case_insensitive) (case_insensitive (lit "id"))))
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
                   (from (id x case_insensitive))
                 )
                 (case_insensitive (lit "a"))
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
                   (from (id x case_insensitive))
                   (where (id b case_insensitive))
                 )
                 (case_insensitive (lit "a"))
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
             (from (id a case_insensitive))
             (limit (lit 10))
           )
        """,
        "SELECT * FROM a LIMIT 10"
    )

    @Test
    fun selectWhereLimit() = assertExpression(
        """(select
             (project (*))
             (from (id a case_insensitive))
             (where (= (id a case_insensitive) (lit 5)))
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
             (+ (lit 5) (id a case_insensitive))
             (type character_varying 1)
           )
        """,
        "CAST(5 + a AS VARCHAR(1))"
    )

    @Test
    fun searchedCaseSingleNoElse() = assertExpression(
        """(searched_case
             (when
               (= (id name case_insensitive) (lit "zoe"))
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
               (= (id name case_insensitive) (lit "zoe"))
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
               (= (id name case_insensitive) (lit "zoe"))
               (lit 1)
             )
             (when
               (> (id name case_insensitive) (lit "kumo"))
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
             (id name case_insensitive)
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
             (id name case_insensitive)
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
             (id name case_insensitive)
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
             (id a case_insensitive)
             (list (lit 1) (lit 2) (lit 3) (lit 4))
           )
        """,
        "a IN (1, 2, 3, 4)"
    )

    @Test
    fun notInOperatorWithImplicitValues() = assertExpression(
        """(not_in
             (id a case_insensitive)
             (list (lit 1) (lit 2) (lit 3) (lit 4))
           )
        """,
        "a NOT IN (1, 2, 3, 4)"
    )

    @Test
    fun inOperatorWithImplicitValuesRowConstructor() = assertExpression(
        """(in
             (list (id a case_insensitive) (id b case_insensitive))
             (list (list (lit 1) (lit 2)) (list (lit 3) (lit 4)))
           )
        """,
        "(a, b) IN ((1, 2), (3, 4))"
    )

    @Test
    fun groupBySingleId() = assertExpression(
        """(select
             (project (list (id a case_insensitive)))
             (from (id data case_insensitive))
             (group (by (id a case_insensitive)))
           )
        """,
        "SELECT a FROM data GROUP BY a"
    )

    @Test
    fun groupBySingleExpr() = assertExpression(
        """(select
             (project (list (+ (id a case_insensitive) (id b case_insensitive))))
             (from (id data case_insensitive))
             (group (by (+ (id a case_insensitive) (id b case_insensitive))))
           )
        """,
        "SELECT a + b FROM data GROUP BY a + b"
    )

    @Test
    fun groupPartialByMultiAliasedAndGroupAliased() = assertExpression(
        """(select
             (project (list (id g case_insensitive)))
             (from (id data case_insensitive))
             (group_partial
               (by
                 (as x (id a case_insensitive))
                 (as y (+ (id b case_insensitive) (id c case_insensitive)))
                 (as z (call foo (id d case_insensitive)))
               )
               (name g)
             )
           )
        """,
        "SELECT g FROM data GROUP PARTIAL BY a AS x, b + c AS y, foo(d) AS z GROUP AS g"
    )

    @Test
    fun havingMinimal() = assertExpression(
        """
          (select
            (project (list (id a case_insensitive)))
            (from (id data case_insensitive))
            (having (= (id a case_insensitive) (id b case_insensitive)))
          )
        """,
        "SELECT a FROM data HAVING a = b"
    )

    @Test
    fun havingWithWhere() = assertExpression(
        """
          (select
            (project (list (id a case_insensitive)))
            (from (id data case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
            (having (= (id c case_insensitive) (id d case_insensitive)))
          )
        """,
        "SELECT a FROM data WHERE a = b HAVING c = d"
    )

    @Test
    fun havingWithWhereAndGroupBy() = assertExpression(
        """
          (select
            (project (list (id g case_insensitive)))
            (from (id data case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
            (group (by (id c case_insensitive) (id d case_insensitive)) (name g))
            (having (> (id d case_insensitive) (lit 6)))
          )
        """,
        "SELECT g FROM data WHERE a = b GROUP BY c, d GROUP AS g HAVING d > 6"
    )

    @Test
    fun pivotWithOnlyFrom() = assertExpression(
        """
          (pivot
            (member (id n case_insensitive) (id v case_insensitive))
            (from (id data case_insensitive))
          )
        """,
        "PIVOT v AT n FROM data"
    )

    @Test
    fun pivotHavingWithWhereAndGroupBy() = assertExpression(
        """
          (pivot
            (member (|| (lit "prefix:") (id c case_insensitive)) (id g case_insensitive))
            (from (id data case_insensitive))
            (where (= (id a case_insensitive) (id b case_insensitive)))
            (group (by (id c case_insensitive) (id d case_insensitive)) (name g))
            (having (> (id d case_insensitive) (lit 6)))
          )
        """,
        "PIVOT g AT ('prefix:' || c) FROM data WHERE a = b GROUP BY c, d GROUP AS g HAVING d > 6"
    )

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
        (select (project (list (id a case_insensitive))) (from (id data case_insensitive)) (where (like (id a case_insensitive) (lit "_AAA%"))))
        """,
        "SELECT a FROM data WHERE a LIKE '_AAA%'"
    )

    @Test
    fun likeColNameLikeColName() = assertExpression(
        """
        (select (project (list (id a case_insensitive) (id b case_insensitive))) (from (id data case_insensitive)) (where (like (id a case_insensitive) (id b case_insensitive))))
        """,
        "SELECT a, b FROM data WHERE a LIKE b"
    )

    @Test
    fun likeColNameLikeColNameDot() = assertExpression(
        """
        (select (project (*)) (from (as a (id data case_insensitive))) (where (like (path (id a case_insensitive) (case_insensitive (lit "name"))) (path (id b case_insensitive) (case_insensitive (lit "pattern"))))))
        """,
        "SELECT * FROM data as a WHERE a.name LIKE b.pattern"
    )

    @Test
    fun likeColNameLikeColNamePqth() = assertExpression(
        """
        (select (project (*)) (from (as a (id data case_insensitive))) (where (like (path (id a case_insensitive) (case_insensitive (lit "name"))) (path (id b case_insensitive) (case_insensitive (lit "pattern"))))))
        """,
        "SELECT * FROM data as a WHERE a.name LIKE b.pattern"
    )

    @Test
    fun likeColNameLikeStringEscape() = assertExpression(
        """
        (select (project (list (id a case_insensitive))) (from (id data case_insensitive)) (where (like (id a case_insensitive) (lit "_AAA%") (lit "["))))
        """,
        "SELECT a FROM data WHERE a LIKE '_AAA%' ESCAPE '[' "
    )

    @Test
    fun notLikeColNameLikeString() = assertExpression(
        """
        (select (project (list (id a case_insensitive))) (from (id data case_insensitive)) (where (not_like (id a case_insensitive) (lit "_AAA%"))))
        """,
        "SELECT a FROM data WHERE a NOT LIKE '_AAA%'"
    )

    @Test
    fun likeColNameLikeColNameEscape() = assertExpression(
        """
        (select (project (list (id a case_insensitive) (id b case_insensitive))) (from (id data case_insensitive)) (where (like (id a case_insensitive) (id b case_insensitive) (lit "\\"))))
        """, //  escape \ inside a Kotlin/Java String
        "SELECT a, b FROM data WHERE a LIKE b ESCAPE '\\'" // escape \ inside a Kotlin/Java String
    )

    @Test
    fun likeColNameLikeColNameEscapeNonLit() = assertExpression(
        """
        (select (project (list (id a case_insensitive) (id b case_insensitive))) (from (id data case_insensitive)) (where (like (id a case_insensitive) (id b case_insensitive) (id c case_insensitive))))
        """, //  escape \ inside a Kotlin/Java String
        "SELECT a, b FROM data WHERE a LIKE b ESCAPE c"
    )

    @Test
    fun likeColNameLikeColNameEscapePath() = assertExpression(
        """
        (select (project (list (id a case_insensitive) (id b case_insensitive))) (from (as x (id data case_insensitive))) (where (like (id a case_insensitive) (id b case_insensitive) (path (id x case_insensitive) (case_insensitive (lit "c"))))))
        """, //  escape \ inside a Kotlin/Java String
        "SELECT a, b FROM data as x WHERE a LIKE b ESCAPE x.c"


    )

    /*
    From SQL92 Spec
     3) "M NOT LIKE P" is equivalent to "NOT (M LIKE P)".
     */
    @Test
    fun likeNotEquivalent() = assertExpression(
        "(select (project (list (id a case_insensitive) (id b case_insensitive))) (from (id data case_insensitive)) (where (not (like (id a case_insensitive) (id b case_insensitive)))))",
        "SELECT a, b FROM data WHERE NOT (a LIKE b)"
    )

    @Test
    fun datePartYear() = assertExpression(
        "(lit \"year\")",
        "year")

    @Test
    fun datePartMonth() = assertExpression(
        "(lit \"month\")",
        "month")

    @Test
    fun datePartDay() = assertExpression(
        "(lit \"day\")",
        "day")

    @Test
    fun datePartHour() = assertExpression(
        "(lit \"hour\")",
        "hour")

    @Test
    fun datePartMinutes() = assertExpression(
        "(lit \"minute\")",
        "minute")

    @Test
    fun datePartSeconds() = assertExpression(
        "(lit \"second\")",
        "second")

    @Test
    fun datePartTimestampHour() = assertExpression(
        "(lit \"timezone_hour\")",
        "timezone_hour")

    @Test
    fun datePartTimezoneMinute() = assertExpression(
        "(lit \"timezone_minute\")",
        "timezone_minute")

    @Test
    fun callDateAddYear() = assertExpression(
        "(call date_add (lit \"year\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(year, a, b)"
    )

    @Test
    fun callDateAddMonth() = assertExpression(
        "(call date_add (lit \"month\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(month, a, b)"
    )

    @Test
    fun callDateAddDay() = assertExpression(
        "(call date_add (lit \"day\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(day, a, b)"
    )

    @Test
    fun callDateAddHour() = assertExpression(
        "(call date_add (lit \"hour\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(hour, a, b)"
    )

    @Test
    fun callDateAddMinute() = assertExpression(
        "(call date_add (lit \"minute\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(minute, a, b)"
    )

    @Test
    fun callDateAddSecond() = assertExpression(
        "(call date_add (lit \"second\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(second, a, b)"
    )

    @Test // invalid evaluation, but valid parsing
    fun callDateAddTwoArguments() = assertExpression(
        "(call date_add (lit \"second\") (id a case_insensitive))",
        "date_add(second, a)"
    )

    @Test // invalid evaluation, but valid parsing
    fun callDateAddTimezoneHour() = assertExpression(
        "(call date_add (lit \"timezone_hour\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(timezone_hour, a, b)")

    @Test // invalid evaluation, but valid parsing
    fun callDateAddTimezoneMinute() = assertExpression(
        "(call date_add (lit \"timezone_minute\") (id a case_insensitive) (id b case_insensitive))",
        "date_add(timezone_minute, a, b)")

    @Test
    fun caseInsensitiveFunctionName() = assertExpression(
        "(call my_function (id a case_insensitive))",
        "mY_fUnCtIoN(a)")

    @Test
    fun callExtractYear() = assertExpression("(call extract (lit \"year\") (id a case_insensitive))",
                                             "extract(year from a)")

    @Test
    fun callExtractMonth() = assertExpression("(call extract (lit \"month\") (id a case_insensitive))",
                                              "extract(month from a)")

    @Test
    fun callExtractDay() = assertExpression("(call extract (lit \"day\") (id a case_insensitive))",
                                            "extract(day from a)")

    @Test
    fun callExtractHour() = assertExpression("(call extract (lit \"hour\") (id a case_insensitive))",
                                             "extract(hour from a)")

    @Test
    fun callExtractMinute() = assertExpression("(call extract (lit \"minute\") (id a case_insensitive))",
                                               "extract(minute from a)")

    @Test
    fun callExtractSecond() = assertExpression("(call extract (lit \"second\") (id a case_insensitive))",
                                               "extract(second from a)")

    @Test
    fun callExtractTimezoneHour() = assertExpression("(call extract (lit \"timezone_hour\") (id a case_insensitive))",
                                               "extract(timezone_hour from a)")

    @Test
    fun callExtractTimezoneMinute() = assertExpression("(call extract (lit \"timezone_minute\") (id a case_insensitive))",
                                               "extract(timezone_minute from a)")

    @Test
    fun semicolonAtEndOfQuery() = assertExpression("(select (project (*)) (from (bag (lit 1))))",
                                                   "SELECT * FROM <<1>>;")

    @Test
    fun semicolonAtEndOfQueryHasNoEffect() {
        val query = "SELECT * FROM <<1>>"
        val withSemicolon = parse("$query;").filterMetaNodes()
        val withoutSemicolon = parse(query).filterMetaNodes()

        assertEquals(withoutSemicolon, withSemicolon)
    }

    @Test
    fun semicolonAtEndOfLiteralHasNoEffect() {
        val withSemicolon = parse("1;").filterMetaNodes()
        val withoutSemicolon = parse("1").filterMetaNodes()

        assertEquals(withoutSemicolon, withSemicolon)
    }

    @Test
    fun semicolonAtEndOfExpressionHasNoEffect() {
        val withSemicolon = parse("(1+1);").filterMetaNodes()
        val withoutSemicolon = parse("(1+1)").filterMetaNodes()

        assertEquals(withoutSemicolon, withSemicolon)
    }
}