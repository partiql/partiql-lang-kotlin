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

    @Test
    fun pathDotOnly() = assertEval("a.b.c.d.e", "5")

    @Test
    fun pathIndexing() = assertEval("stores[0].books[2].title", "\"C\"")

    @Test
    fun pathParent() = assertEval("stores[0].books[2].title....books[3].title", "\"D\"")

    @Test
    fun pathWildcard() = assertEval("stores[0].books.*.title", """["A", "B", "C", "D"]""")

    @Test
    fun pathDoubleWildCard() = assertEval(
        "stores.*.books.*.title",
        """["A", "B", "C", "D", "A", "E", "F"]"""
    )

    @Test
    fun pathDoubleWildCardWithParent() = assertEval(
        "stores.*.books.*.categories..title",
        """["A", "B", "C", "D", "A", "E", "F"]"""
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
    )

    @Test
    fun implicitAliasSelectSingleSource() = assertEval(
        """SELECT id FROM stores""",
        """[{id:"5"}, {id:"6"}]"""
    )

    @Test
    fun selectStarSingleSourceHoisted() = assertEval(
        """SELECT * FROM stores.books.* AS b WHERE b.price >= 9.0""",
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
        """SELECT title AS name, price FROM stores.books.* AS b WHERE b.price >= 9.0""",
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
        """SELECT id AS name FROM stores WHERE id == "5" """,
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
        """SELECT * FROM animals, animal_types WHERE type == id""",
        """
          [
            {name: "Kumo", type: "dog", id: "dog", is_magic: false},
            {name: "Mochi", type: "dog", id: "dog", is_magic: false},
            {name: "Lilikoi", type: "unicorn", id: "unicorn", is_magic: true},
          ]
        """
    )

    @Test
    fun nestedSelectJoin() = assertEval(
        """
          SELECT ${'$'}name AS col, ${'$'}value AS val
          FROM (SELECT * FROM animals, animal_types WHERE type == id).*
          WHERE ${'$'}name != "id"
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
          SELECT ${'$'}name AS col, ${'$'}value AS val
          FROM (SELECT * FROM animals, animal_types WHERE type == id).*
          WHERE ${'$'}name != "id"
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
}

