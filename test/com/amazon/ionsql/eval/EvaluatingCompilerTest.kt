/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.Base
import com.amazon.ionsql.syntax.ParserException
import com.amazon.ionsql.util.exprValue
import org.junit.Test

class EvaluatingCompilerTest : Base() {
    val evaluator = EvaluatingCompiler(ion)

    fun eval(source: String): ExprValue =
        evaluator
            .compile(source)
            .eval(
                Bindings.over {
                    when (it) {
                        "a" -> literal("{b:{c:{d:{e:5, f:6}}}}").exprValue()
                        "i" -> literal("1").exprValue()
                        "f" -> literal("2e0").exprValue()
                        "d" -> literal("3d0").exprValue()
                        "s" -> literal("\"hello\"").exprValue()
                        "animals" -> literal(
                            """
                            [
                              {name: "Kumo", type: "dog"},
                              {name: "Mochi", type: "dog"},
                              {name: "Lilikoi", type: "unicorn"},
                            ]
                            """).exprValue()
                        "animal_types" -> literal(
                            """
                            [
                              {id: "dog", is_magic: false},
                              {id: "cat", is_magic: false},
                              {id: "unicorn", is_magic: true},
                            ]
                            """).exprValue()
                        "stores" -> literal(
                            """
                            [
                              {
                                id: "5",
                                books: [
                                  {title:"A", price: 5.0, categories:["sci-fi", "action"]},
                                  {title:"B", price: 2.0, categories:["sci-fi", "comedy"]},
                                  {title:"C", price: 7.0, categories:["action", "suspense"]},
                                  {title:"D", price: 9.0, categories:["suspense"]},
                                ]
                              },
                              {
                                id: "6",
                                books: [
                                  {title:"A", price: 5.0, categories:["sci-fi", "action"]},
                                  {title:"E", price: 9.5, categories:["fantasy", "comedy"]},
                                  {title:"F", price: 10.0, categories:["history"]},
                                ]
                              }
                            ]
                            """).exprValue()
                        "friends" -> literal(
                            """
                            {
                               kumo: {
                                 type: "dog",
                                 likes: {
                                   mochi: { type: "dog" },
                                   zoe: { type: "human" },
                                 }
                               },
                               mochi: {
                                 type: "dog",
                                 likes: {
                                   kumo: { type: "dog" },
                                   brownie: { type: "cat" },
                                 }
                               },
                            }
                            """).exprValue()
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

    @Test(expected = ParserException::class)
    fun emptyThrows() = voidEval("")

    @Test
    fun literal() = assertEval("5", "5")

    @Test
    fun identifier() = assertEval("i", "1")

    @Test
    fun lexicalScope() = assertEval("@i", "1")

    @Test
    fun functionCall() = assertEval("exists(select * from [1])", "true")

    @Test
    fun grouping() = assertEval("((i))", "1")

    @Test
    fun listLiteral() = assertEval("[i, f, d]", "[1, 2e0, 3d0]")

    @Test
    fun rowValueConstructor() = assertEval("(i, f, d)", "[1, 2e0, 3d0]")

    @Test
    fun structLiteral() = assertEval("{'a':i, 'b':f, 'c':d, 'd': 1}", "{a:1, b:2e0, c:3d0, d:1}") {
        // struct literals provide ordered names
        val bindNames = exprValue.asFacet(OrderedBindNames::class.java)!!
        assertEquals(listOf("a", "b", "c", "d"), bindNames.orderedNames)
    }

    @Test
    fun bagLiteral() = assertEval("<<i, f, d>>", "[1, 2e0, 3d0]")

    @Test
    fun tableValueConstructor() = assertEval("VALUES (i), (f, d)", "[[1], [2e0, 3d0]]")

    @Test
    fun emptyListLiteral() = assertEval("[]", "[]")

    @Test
    fun emptyStructLiteral() = assertEval("{}", "{}")

    @Test
    fun emptyBagLiteral() = assertEval("<<>>", "[]")

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
    fun moreIntFloat() = assertEval("3 > `2e0`", "true")

    @Test
    fun moreIntFloatFalse() = assertEval("1 > `2e0`", "false")

    @Test
    fun lessIntFloat() = assertEval("1 < `2e0`", "true")

    @Test
    fun lessIntFloatFalse() = assertEval("3 < `2e0`", "false")

    @Test
    fun moreEqIntFloat() = assertEval("3 >= `2e0`", "true")

    @Test
    fun moreEqIntFloatFalse() = assertEval("1 >= `2e0`", "false")

    @Test
    fun lessEqIntFloat() = assertEval("1 <= `2e0`", "true")

    @Test
    fun lessEqIntFloatFalse() = assertEval("5 <= `2e0`", "false")

    @Test
    fun equalIntFloat() = assertEval("1 = 1e0", "true")

    @Test
    fun equalIntFloatFalse() = assertEval("1 = 1e1", "false")

    @Test
    fun equalListDifferentTypesTrue() = assertEval(
        """[1, `2e0`, 'hello'] = [1.0, 2, `hello`]""",
        "true"
    )

    @Test
    fun equalListDifferentLengthsShortFirst() = assertEval(
        """[1.0, 2] = [1.0, 2, `hello`]""",
        "false"
    )

    @Test
    fun equalListDifferentLengthsLongFirst() = assertEval(
        """[1, `2e0`, 'hello'] = [1, `2e0`]""",
        "false"
    )

    @Test
    fun notEqualIntFloat() = assertEval("1 != `2e0`", "true")

    @Test
    fun notEqualIntFloatFalse() = assertEval("1 != `1e0`", "false")

    @Test
    fun missingIsMissing() = assertEval("MISSING IS MISSING", "true")

    @Test
    fun missingIsNull() = assertEval("MISSING IS NULL", "true")

    @Test
    fun nullIsNotMissing() = assertEval("NULL IS NOT MISSING", "true")

    @Test
    fun nullIsNotNull() = assertEval("NULL IS NOT NULL", "false")

    @Test
    fun typedNullIsNull() = assertEval("`null.string` IS NOT NULL", "false")

    @Test
    fun stringIsVarchar() = assertEval("'hello' IS CHARACTER VARYING", "true")

    @Test
    fun stringIsString() = assertEval("'hello' IS STRING", "true")

    @Test
    fun intIsNotSmallInt() = assertEval("50000 IS NOT SMALLINT", "false")

    @Test(expected = EvaluationException::class)
    fun notOnNonBooleanThrows() = voidEval("not i")

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

    @Test
    fun pathDotOnly() = assertEval("a.b.c.d.e", "5")

    @Test
    fun pathIndexing() = assertEval("stores[0].books[2].title", "\"C\"")

    @Test
    fun pathWildcard() = assertEval("stores[0].books[*].title", """["A", "B", "C", "D"]""")

    @Test
    fun pathUnpivotWildcard() = assertEval("friends.kumo.likes.*.type", """["dog", "human"]""")

    @Test
    fun pathDoubleWildCard() = assertEval(
        "stores[*].books[*].title",
        """["A", "B", "C", "D", "A", "E", "F"]"""
    )

    @Test
    fun pathDoubleUnpivotWildCard() = assertEval(
        "friends.*.likes.*.type",
        """["dog", "human", "dog", "cat"]"""
    )

    @Test
    fun pathWildCardOverScalar() = assertEval(
        "s[*]",
        """["hello"]"""
    )

    @Test
    fun pathUnpivotWildCardOverScalar() = assertEval(
        "s.*",
        """["hello"]"""
    )

    @Test
    fun pathWildCardOverScalarMultiple() = assertEval(
        "(100)[*][*][*]",
        """[100]"""
    )

    @Test
    fun pathUnpivotWildCardOverScalarMultiple() = assertEval(
        "(100).*.*.*",
        """[100]"""
    )

    @Test
    fun selectFromScalarAndAtUnpivotWildCardOverScalar() = assertEval(
        "SELECT VALUE [n, v] FROM (100).* AS v AT n",
        """[
          ["_1", 100]
        ]"""
    )

    @Test
    fun selectFromListAndAtUnpivotWildCardOverScalar() = assertEval(
        "SELECT VALUE [n, (SELECT VALUE [i, x] FROM @v AS x AT i)] FROM [100, 200].*.*.* AS v AT n",
        """[
          ["_1", [[0, 100], [1, 200]]]
        ]"""
    )

    @Test
    fun selectFromBagAndAtUnpivotWildCardOverScalar() = assertEval(
        """
          SELECT VALUE
            [n, (SELECT VALUE [i IS MISSING, i, x] FROM @v AS x AT i)]
          FROM <<100, 200>>.* AS v AT n
        """,
        """[
          ["_1", [[true, null, 100], [true, null, 200]]]
        ]"""
    )

    @Test
    fun pathWildCardOverStructMultiple() = assertEval(
        "a[*][*][*][*]",
        """[{b:{c:{d:{e:5, f:6}}}}]"""
    )

    @Test
    fun pathUnpivotWildCardOverStructMultiple() = assertEval(
        "a.*.*.*.*",
        """[5, 6]"""
    )

    @Test
    fun selectPathUnpivotWildCardOverStructMultiple() = assertEval(
        "SELECT name, val FROM a.*.*.*.* AS val AT name",
        """[{name: "e", val: 5}, {name: "f", val: 6}]"""
    )

    @Test
    fun rangeOverScalar() = assertEval(
        "SELECT VALUE v FROM 1 AS v",
        """[1]"""
    )

    @Test
    fun rangeTwiceOverScalar() = assertEval(
        "SELECT VALUE [v1, v2] FROM 1 AS v1, @v1 AS v2",
        """[[1, 1]]"""
    )

    @Test
    fun rangeOverSexp() = assertEval(
        "SELECT VALUE v FROM `(a b c)` AS v",
        """[(a b c)]"""
    )

    @Test
    fun rangeOverStruct() = assertEval(
        "SELECT VALUE v FROM `{a:5}` AS v",
        """[{a:5}]"""
    )

    @Test
    fun rangeOverList() = assertEval(
        "SELECT VALUE v FROM `[1, 2, 3]` AS v",
        """[1, 2, 3]"""
    )

    @Test
    fun rangeOverListWithAt() = assertEval(
        "SELECT VALUE i FROM `[1, 2, 3]` AT i",
        """[0, 1, 2]"""
    )

    @Test
    fun rangeOverListWithAsAndAt() = assertEval(
        "SELECT VALUE [i, v] FROM `[1, 2, 3]` AS v AT i",
        """[[0, 1], [1, 2], [2, 3]]"""
    )

    @Test
    fun rangeOverBagWithAt() = assertEval(
        "SELECT VALUE [i, v] FROM <<1, 2, 3>> AS v AT i",
        """[[null, 1], [null, 2], [null, 3]]"""
    )

    @Test
    fun rangeOverNestedWithAt() = assertEval(
        "SELECT VALUE [i, v] FROM (SELECT VALUE v FROM `[1, 2, 3]` AS v) AS v AT i",
        """[[null, 1], [null, 2], [null, 3]]"""
    )

    @Test
    fun selectStarSingleSource() = assertEval(
        """SELECT * FROM animals""",
        """
          [
            {name: "Kumo", type: "dog"},
            {name: "Mochi", type: "dog"},
            {name: "Lilikoi", type: "unicorn"},
          ]
        """
    ) {
        // SELECT * from schema-less Ion provides no ordered names
        exprValue.forEach {
            assertNull(it.asFacet(OrderedBindNames::class.java))
        }
    }

    @Test
    fun implicitAliasSelectSingleSource() = assertEval(
        """SELECT id FROM stores""",
        """[{id:"5"}, {id:"6"}]"""
    ) {
        // SELECT list provides ordered names facet
        exprValue.forEach {
            val bindNames = it.asFacet(OrderedBindNames::class.java)!!
            assertEquals(listOf("id"), bindNames.orderedNames)
        }
    }

    @Test
    fun selectValues() = assertEval(
        """SELECT VALUE id FROM stores""",
        """["5", "6"]"""
    )

    @Test
    fun variableShadow() = assertEval(
        // Note that i, f, d, and s are defined in the global environment
        """SELECT f, d, s FROM i AS f, f AS d, @f AS s WHERE f = 1 AND d = 2e0 and s = 1""",
        """[{f: 1, d: 2e0, s: 1}]"""
    )

    @Test
    fun selectStarSingleSourceHoisted() = assertEval(
        """SELECT * FROM stores[*].books[*] AS b WHERE b.price >= 9.0""",
        """
          [
            {title:"D", price: 9.0, categories:["suspense"]},
            {title:"E", price: 9.5, categories:["fantasy", "comedy"]},
            {title:"F", price: 10.0, categories:["history"]},
          ]
        """
    )

    @Test
    fun explicitAliasSelectSingleSource() = assertEval(
        """SELECT id AS name FROM stores""",
        """[{name:"5"}, {name:"6"}]"""
    )

    @Test
    fun selectImplicitAndExplicitAliasSingleSourceHoisted() = assertEval(
        """SELECT title AS name, price FROM stores[*].books[*] AS b WHERE b.price >= 9.0""",
        """
          [
            {name:"D", price: 9.0},
            {name:"E", price: 9.5},
            {name:"F", price: 10.0},
          ]
        """
    )

    @Test
    fun explicitAliasSelectSingleSourceWithWhere() = assertEval(
        """SELECT id AS name FROM stores WHERE id = '5' """,
        """[{name:"5"}]"""
    )

    @Test
    fun selectCrossProduct() = assertEval(
        """SELECT * FROM animals, animal_types""",
        """
          [
            {name: "Kumo", type: "dog", id: "dog", is_magic: false},
            {name: "Kumo", type: "dog", id: "cat", is_magic: false},
            {name: "Kumo", type: "dog", id: "unicorn", is_magic: true},

            {name: "Mochi", type: "dog", id: "dog", is_magic: false},
            {name: "Mochi", type: "dog", id: "cat", is_magic: false},
            {name: "Mochi", type: "dog", id: "unicorn", is_magic: true},

            {name: "Lilikoi", type: "unicorn", id: "dog", is_magic: false},
            {name: "Lilikoi", type: "unicorn", id: "cat", is_magic: false},
            {name: "Lilikoi", type: "unicorn", id: "unicorn", is_magic: true},
          ]
        """
    )

    @Test
    fun selectJoin() = assertEval(
        """SELECT * FROM animals, animal_types WHERE type = id""",
        """
          [
            {name: "Kumo", type: "dog", id: "dog", is_magic: false},
            {name: "Mochi", type: "dog", id: "dog", is_magic: false},
            {name: "Lilikoi", type: "unicorn", id: "unicorn", is_magic: true},
          ]
        """
    )

    @Test
    fun selectCorrelatedJoin() = assertEval(
        """SELECT s.id AS id, b.title AS title FROM stores AS s, @s.books AS b WHERE b.price > 5""",
        """
          [
            {id: "5", title: "C"},
            {id: "5", title: "D"},
            {id: "6", title: "E"},
            {id: "6", title: "F"},
          ]
        """
    )

    @Test
    fun selectNonCorrelatedJoin() = assertEval(
        // Note that the joined s is coming from the global scope without @-operator
        """SELECT s.id AS id, v AS title FROM stores AS s, s AS v""",
        """
          [
            {id: "5", title: "hello"},
            {id: "6", title: "hello"},
          ]
        """
    )

    @Test
    fun selectCorrelatedUnpivot() = assertEval(
        """
          SELECT n1, n2, n3, n4, val
          FROM UNPIVOT a AS b AT n1,
               UNPIVOT @b AS c AT n2,
               UNPIVOT @c AS d AT n3,
               UNPIVOT @d AS val AT n4
        """,
        """
          [
            {n1: "b", n2: "c", n3: "d", n4: "e", val: 5},
            {n1: "b", n2: "c", n3: "d", n4: "f", val: 6}
          ]
        """
    )

    @Test
    fun nestedSelectJoinWithUnpivot() = assertEval(
        """
          SELECT col, val
          FROM (SELECT * FROM animals, animal_types WHERE type = id) AS a,
               UNPIVOT @a AS val AT col
          WHERE col != 'id'
        """,
        """
          [
            {col: "name", val: "Kumo"},
            {col: "type", val: "dog"},
            {col: "is_magic", val: false},

            {col: "name", val: "Mochi"},
            {col: "type", val: "dog"},
            {col: "is_magic", val: false},

            {col: "name", val: "Lilikoi"},
            {col: "type", val: "unicorn"},
            {col: "is_magic", val: true},
          ]
        """
    )

    @Test
    fun nestedSelectJoinLimit() = assertEval(
        """
          SELECT col, val
          FROM (SELECT * FROM animals, animal_types WHERE type = id) AS a,
               UNPIVOT @a AS val AT col
          WHERE col != 'id'
          LIMIT 6 - 3
        """,
        """
          [
            {col: "name", val: "Kumo"},
            {col: "type", val: "dog"},
            {col: "is_magic", val: false},
          ]
        """
    )

    @Test
    fun pivotFrom() = assertEval(
        """
          PIVOT a."type" AT a.name FROM animals AS a
        """,
        """
          {
            Kumo: "dog",
            Mochi: "dog",
            Lilikoi: "unicorn",
          }
        """
    )

    @Test
    fun pivotLiteralFieldNameFrom() = assertEval(
        """
          PIVOT a.name AT 'name' FROM animals AS a
        """,
        """
          {
            name: "Kumo",
            name: "Mochi",
            name: "Lilikoi",
          }
        """
    )

    @Test
    fun pivotBadFieldType() = assertEval(
        """
          PIVOT a.name AT i FROM animals AS a AT i
        """,
        """
          {}
        """
    )

    @Test
    fun pivotUnpivotWithWhereLimit() = assertEval(
        """
          PIVOT val AT 'new_' || name
          FROM UNPIVOT `{a:1, b:2, c:3, d:4, e:5, f: 6}` AS val AT name
          WHERE name <> 'b' AND val <> 3
          LIMIT 3
        """,
        """
          {
            new_a: 1,
            new_d: 4,
            new_e: 5,
          }
        """
    )

    @Test
    fun syntheticColumnNameInSelect() = assertEval(
        """SELECT i+1 FROM <<100>> i""",
        """[{_1: 101}]"""
    )

    @Test
    fun properAliasFromPathInSelect() = assertEval(
        """
          SELECT s.id, s.books[*].title FROM stores AS s WHERE s.id = '5'
        """,
        """
          [
            {
              id: "5",
              title: ["A", "B", "C", "D"]
            }
          ]
        """
    )

    @Test
    fun inPredicate() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price IN (5, `2e0`)
        """,
        """
          [
            "A", "B", "A"
          ]
        """
    )

    @Test
    fun notInPredicate() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price NOT IN (5, `2e0`)
        """,
        """
          [
            "C", "D", "E", "F"
          ]
        """
    )

    @Test
    fun inPredicateWithTableConstructor() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b
          WHERE (b.title, b.price) IN (VALUES ('A', `5e0`), ('B', 3.0), ('X', 9.0))
        """,
        """
          [
            "A", "A"
          ]
        """
    )

    @Test
    fun notInPredicateWithTableConstructor() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b
          WHERE (b.title, b.price) NOT IN (VALUES ('A', `5e0`), ('B', 3.0), ('X', 9.0))
        """,
        """
          [
            "B", "C", "D", "E", "F"
          ]
        """
    )

    @Test
    fun inPredicateWithExpressionOnRightSide() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b
          WHERE 'comedy' IN b.categories
        """,
        """
          [
            "B", "E"
          ]
        """
    )

    @Test
    fun notInPredicateWithExpressionOnRightSide() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b
          WHERE 'comedy' NOT IN b.categories
        """,
        """
          [
            "A", "C", "D", "A", "F"
          ]
        """
    )

    @Test
    fun simpleCase() = assertEval(
        """
          SELECT VALUE
            CASE x + 1
              WHEN i THEN 'ONE'
              WHEN f THEN 'TWO'
              WHEN d THEN 'THREE'
              ELSE '?'
            END
          FROM << i, f, d >> AS x
        """,
        """
          [
            "TWO", "THREE", "?"
          ]
        """
    )

    @Test
    fun simpleCaseNoElse() = assertEval(
        """
          SELECT VALUE
            CASE x + 1
              WHEN i THEN 'ONE'
              WHEN f THEN 'TWO'
              WHEN d THEN 'THREE'
            END
          FROM << i, f, d >> AS x
        """,
        """
          [
            "TWO", "THREE", null
          ]
        """
    )

    @Test
    fun searchedCase() = assertEval(
        """
          SELECT VALUE
            CASE
              WHEN x + 1 < i THEN '< ONE'
              WHEN x + 1 = f THEN 'TWO'
              WHEN (x + 1 > d) AND (x + 1 < 100) THEN '>= THREE < 100'
              ELSE '?'
            END
          FROM << -1.0000, i, f, d, 100e0 >> AS x
        """,
        """
          [
            "< ONE", "TWO", "?", ">= THREE < 100", "?"
          ]
        """
    )

    @Test
    fun searchedCaseNoElse() = assertEval(
        """
          SELECT VALUE
            CASE
              WHEN x + 1 < i THEN '< ONE'
              WHEN x + 1 = f THEN 'TWO'
              WHEN (x + 1 > d) AND (x + 1 < 100) THEN '>= THREE < 100'
            END
          FROM << -1.0000, i, f, d, 100e0 >> AS x
        """,
        """
          [
            "< ONE", "TWO", null, ">= THREE < 100", null
          ]
        """
    )

    @Test
    fun betweenPredicate() = assertEval(
        """
          SELECT VALUE x
          FROM << -1.0000, i, f, d, 100e0 >> AS x
          WHERE x BETWEEN 1.000001 AND 3.0000000
        """,
        """
          [
            2e0, 3d0
          ]
        """
    )

    @Test
    fun notBetweenPredicate() = assertEval(
        """
          SELECT VALUE x
          FROM << -1.0000, i, f, d, 100e0 >> AS x
          WHERE x NOT BETWEEN 1.000001 AND 3.0000000
        """,
        """
          [
            -1.0000, 1, 100d0
          ]
        """
    )
}
