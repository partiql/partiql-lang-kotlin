/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import com.amazon.ion.system.IonSystemBuilder
import org.junit.Ignore
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.syntax.ParserException

class EvaluatingCompilerTests : EvaluatorTestBase() {

    private val globalStruct = mapOf(
        "a" to "{b:{c:{d:{e:5, f:6}}}}",
        "b" to "{ c: 100 }"
    )
    private val globalHello = mapOf("s" to "\"hello\"")
    private val globalListOfNumbers = mapOf("numbers" to "[1, 2.0, 3e0, 4, 5d0]")
    private val simpleListOfStructs = mapOf("someList" to "[{a:1}, {a:2}, {a:3}]")

    /**
     * mappings to different number types
     */
    private val globalNumbers = mapOf(
        "i" to "1",
        "f" to "2e0",
        "d" to "3d0")

    /**
     * Sample ion containing a collection of stores
     */
    private val stores = mapOf(
        "stores" to """
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
          },
          {
            id: "7",
            books: []
          }
        ]
        """
    )

    private val storesAndPrices = stores + mapOf(
        "prices" to """[5, 2e0]"""
    )

    private val animals = mapOf(
        "animals" to """
        [
          {name: "Kumo", type: "dog"},
          {name: "Mochi", type: "dog"},
          {name: "Lilikoi", type: "unicorn"},
        ]
        """
    )

    private val animalTypes = mapOf(
        "animal_types" to """
        [
          {id: "dog", is_magic: false},
          {id: "cat", is_magic: false},
          {id: "unicorn", is_magic: true},
        ]
        """
    )

    private val friends = mapOf(
        "friends" to """
        {
           kumo: {
             type: "DOG",
             likes: {
               mochi: { type: "dog" },
               zoe: { type: "human" },
             }
           },
           mochi: {
             type: "DOG",
             likes: {
               kumo: { type: "dog" },
               brownie: { type: "cat" },
             }
           },
        }
        """
    )

    @Test(expected = ParserException::class)
    fun emptyThrows() = voidEval("")

    @Test
    fun literal() = assertEval("5", "5")

    @Test
    fun identifier() = assertEval("i", "1", globalNumbers.toSession())

    @Test
    fun identifierCaseMismatch() = assertEval("I", "1", globalNumbers.toSession())

    @Test
    fun quotedIdentifier() = assertEval("\"i\"", "1", globalNumbers.toSession())

    @Test
    fun lexicalScope() = assertEval("@i", "1", globalNumbers.toSession())

    @Test
    fun functionCall() = assertEval("exists(select * from [1])", "true")

    @Test
    fun grouping() = assertEval("((i))", "1", globalNumbers.toSession())

    @Test
    fun listLiteral() = assertEval("[i, f, d]", "[1, 2e0, 3d0]", globalNumbers.toSession())

    @Test
    fun rowValueConstructor() = assertEval("(i, f, d)", "[1, 2e0, 3d0]", globalNumbers.toSession())

    @Test
    fun structLiteral() = assertEval("{'a':i, 'b':f, 'c':d, 'd': 1}", "{a:1, b:2e0, c:3d0, d:1}", (globalStruct + globalNumbers).toSession()) {
        // struct literals provide ordered names
        val bindNames = exprValue.orderedNames!!
        assertEquals(listOf("a", "b", "c", "d"), bindNames)
    }

    @Test
    fun bagLiteral() = assertEval("<<i, f, d>>", "[1, 2e0, 3d0]", globalNumbers.toSession())

    @Test
    fun tableValueConstructor() = assertEval("VALUES (i), (f, d)", "[[1], [2e0, 3d0]]", globalNumbers.toSession())

    @Test
    fun emptyListLiteral() = assertEval("[]", "[]")

    @Test
    fun emptyStructLiteral() = assertEval("{}", "{}")

    @Test
    fun emptyBagLiteral() = assertEval("<<>>", "[]")

    @Test
    fun unaryPlus() = assertEval("+i", "1", globalNumbers.toSession())

    @Test
    fun unaryMinus() = assertEval("-f", "-2e0", globalNumbers.toSession())

    @Test
    fun logicalNotFalse() = assertEval("not false", "true")

    @Test
    fun logicalNotTrue() = assertEval("not false", "true")

    @Test
    fun binaryAddWith2Terms() = assertEval("1 + 1", "2")

    @Test
    fun binaryAddWith3Terms() = assertEval("1 + 1 + 1", "3")

    @Test
    fun addIntFloat() = assertEval("i + f", "3e0", globalNumbers.toSession())

    @Test
    fun subIntFloatDecimal() = assertEval("i - f - d", "-4.", globalNumbers.toSession())

    @Test
    fun repeatingDecimal() = assertEval("4.0000/3.0", "1.3333333333333333333333333333333333333")

    @Test
    fun repeatingDecimalHigherPrecision() = assertEval("4.000000000000000000000000000000000000/3.0",
                                                       "1.3333333333333333333333333333333333333")
    @Test
    fun decimalRoundUp() = assertEval("1.9999999999999999999999999999999999999999999999",
                                      "2.0000000000000000000000000000000000000")

    @Test
    fun decimalRoundDown() = assertEval("1.00000000000000000000000000000000000000000001",
                                        "1.0000000000000000000000000000000000000")

    @Test
    fun subtractionOutOfAllowedPrecision() = assertEval("1e100 - 1e-100", "10000000000000000000000000000000000000d63")

    @Test
    fun bigDecimals() = assertEval("${Long.MAX_VALUE}.0 + 100.0", "9223372036854775907.0")

    @Test
    fun mulFloatIntInt() = assertEval("f * 2 * 4", "16e0", globalNumbers.toSession())

    @Test
    fun divDecimalInt() = assertEval("d / 2", "1.5", globalNumbers.toSession())

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
    fun symbolEquality() = assertEval(
        """ 'A' = 'A' """,
        "true")

    @Test
    fun symbolCaseEquality() = assertEval(
        """ 'A' = 'a' """,
        "false")

    @Test
    fun notEqualIntInt() = assertEval("1 != 2", "true")

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
    fun comparisonsConjuctTrue() = assertEval("i < f and f < d", "true", globalNumbers.toSession())

    @Test
    fun comparisonsDisjunctFalse() = assertEval(
        "i < f and (f > d or i > d)",
        "false",
        globalNumbers.toSession()
    )

    @Test
    fun pathSimpleDotOnly() = assertEval("b.c", "100", globalStruct.toSession())

    @Test
    fun pathDotOnly() = assertEval("a.b.c.d.e", "5", globalStruct.toSession())

    @Test
    fun pathDotMissingAttribute() = assertEval("a.z IS MISSING", "true", globalStruct.toSession())

    @Test
    fun pathMissingDotName() = assertEval("(MISSING).a IS MISSING", "true")

    @Test
    fun pathNullDotName() = assertEval("(NULL).a IS MISSING", "true")

    @Test
    fun pathIndexing() = assertEval("stores[0].books[2].title", "\"C\"", stores.toSession())

    @Test
    fun pathIndexListLiteral() = assertEval("[1, 2, 3][1]", "2")

    @Test
    fun pathIndexBagLiteral() = assertEval("<<1, 2, 3>>[1]", "null") {
        assertEquals(ExprValueType.MISSING, exprValue.type)
    }

    @Test
    fun pathFieldStructLiteral() = assertEval("{'a': 1, 'b': 2, 'b': 3}.a", "1")

    @Test
    fun pathIndexStructLiteral() = assertEval("{'a': 1, 'b': 2, 'b': 3}[1]", "2")

    @Test
    fun pathIndexStructOutOfBoundsLowLiteral() = assertEval("{'a': 1, 'b': 2, 'b': 3}[-1]", "null") {
        assertEquals(ExprValueType.MISSING, exprValue.type)
    }

    @Test
    fun pathIndexStructOutOfBoundsHighLiteral() = assertEval("{'a': 1, 'b': 2, 'b': 3}[3]", "null") {
        assertEquals(ExprValueType.MISSING, exprValue.type)
    }

    @Test
    fun pathUnpivotWildcard() = assertEval("friends.kumo.likes.*", """[{type:"dog"},{type:"human"}]""", friends.toSession())

    @Test
    fun pathUnpivotWildcardFieldsAfter() = assertEval("friends.kumo.likes.*.type", """["dog", "human"]""", friends.toSession())

    @Test
    fun pathSimpleWildcard() = assertEval("someList[*].a", """[1, 2, 3]""", simpleListOfStructs.toSession())





    private val books = mapOf("e" to """[{books:["b1","b2"]}]""",
                              "f" to """[{foobar: 1}]""").toSession()

    @Test
    fun selectValuePath() = assertEval("SELECT VALUE v1.books FROM e AS v1", """[["b1", "b2"]]""", books)

    @Test
    fun pathWildcardPath() = assertEval("e[*].books", """[["b1", "b2"]]""", books)

    @Test
    fun pathWildcard() = assertEval("stores[0].books[*].title", """["A", "B", "C", "D"]""", stores.toSession())


    @Test
    fun pathDoubleWildCard() = assertEval(
        "stores[*].books[*].title",
        """["A", "B", "C", "D", "A", "E", "F"]""",
        stores.toSession()
    )

    @Test
    fun pathDoubleUnpivotWildCard() = assertEval(
        "friends.*.likes.*.type",
        """["dog", "human", "dog", "cat"]""",
        friends.toSession()
    )

    @Test
    fun pathWildCardOverScalar() = assertEval(
        "s[*]",
        """["hello"]""",
        globalHello.toSession()
    )

    @Test
    fun pathUnpivotWildCardOverScalar() = assertEval(
        "s.*",
        """["hello"]""",
        globalHello.toSession()
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
        """[{b:{c:{d:{e:5, f:6}}}}]""",
        globalStruct.toSession()
    )

    @Test
    fun pathUnpivotWildCardOverStructMultiple() = assertEval(
        "a.*.*.*.*",
        """[5, 6]""",
        globalStruct.toSession()
    )

    @Test
    fun selectPathUnpivotWildCardOverStructMultiple() = assertEval(
        "SELECT name, val FROM a.*.*.*.* AS val AT name",
        """[{name: "e", val: 5}, {name: "f", val: 6}]""",
        globalStruct.toSession()
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
    fun rangeOverListConstructorWithAt() = assertEval(
        "SELECT VALUE i FROM [1, 2, 3] AT i",
        """[0, 1, 2]"""
    )

    @Test
    fun rangeOverListConstructorWithAsAndAt() = assertEval(
        "SELECT VALUE [i, v] FROM [1, 2, 3] AS v AT i",
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
        """,
        animals.toSession()
    )

    @Test
    fun implicitAliasSelectSingleSource() = assertEval(
        """SELECT id FROM stores""",
        """[{id:"5"}, {id:"6"}, {id:"7"}]""",
        stores.toSession()
    ) {
        // SELECT list provides ordered names facet
        exprValue.forEach {
            val bindNames = it.orderedNames!!
            assertEquals(listOf("id"), bindNames)
        }
    }

    @Test
    fun selectIndexStruct() = assertEval(
        """SELECT VALUE x[0] FROM (SELECT s.id FROM stores AS s) AS x""",
        """["5", "6", "7"]""",
        stores.toSession()
    )

    @Test
    fun selectValues() = assertEval(
        """SELECT VALUE id FROM stores""",
        """["5", "6", "7"]""",
        stores.toSession()
    )

    @Test
    fun variableShadow() = assertEval(
        // Note that i, f, d, and s are defined in the global environment
        """SELECT f, d, s FROM i AS f, f AS d, @f AS s WHERE f = 1 AND d = 2e0 and s = 1""",
        """[{f: 1, d: 2e0, s: 1}]""",
        globalNumbers.toSession()
    )

    @Test
    fun parameters() = assertEval(
        """SELECT ? as b1, f.bar FROM foo f WHERE f.bar = ?""",
        """[{b1:"spam",bar:"baz"}]""",
        EvaluationSession.build {
            globals(mapOf("foo" to """[{"bar": "baz"}, {"bar": "blargh"}]""").toBindings())
            parameters(listOf("spam", "baz").map { valueFactory.newString(it) })
        }
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
        """,
        stores.toSession()
    )

    @Test
    fun explicitAliasSelectSingleSource() = assertEval(
        """SELECT id AS name FROM stores""",
        """[{name:"5"}, {name:"6"}, {name:"7"}]""",
        stores.toSession()
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
        """,
        stores.toSession()
    )

    @Test
    fun explicitAliasSelectSingleSourceWithWhere() = assertEval(
        """SELECT id AS name FROM stores WHERE id = '5' """,
        """[{name:"5"}]""",
        stores.toSession()
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
        """,
        (animals + animalTypes).toSession()
    )

    @Test
    fun selectWhereStringEqualsSameCase() = assertEval(
        """SELECT * FROM animals as a WHERE a.name = 'Kumo' """,
        """
          [
            {name: "Kumo", type: "dog"}
          ]
        """,
        animals.toSession()
    )

    @Test
    fun selectWhereStrinEqualsDifferentCase() = assertEval(
        """SELECT * FROM animals as a WHERE a.name = 'KUMO' """,
        """
          []
        """,
        animals.toSession()
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
        """,
        (animals + animalTypes).toSession()
    )

    @Test
    fun selectCorrelatedJoin() = assertEval(
        """SELECT s.id AS id, b.title AS title FROM stores AS s, @s.books AS b WHERE b IS NULL OR b.price > 5""",
        """
          [
            {id: "5", title: "C"},
            {id: "5", title: "D"},
            {id: "6", title: "E"},
            {id: "6", title: "F"},
          ]
        """,
        stores.toSession()
    )

    @Test
    fun selectCorrelatedLeftJoin() = assertEval(
        """SELECT s.id AS id, b.title AS title FROM stores AS s LEFT CROSS JOIN @s.books AS b WHERE b IS NULL""",
        """
          [
            {id: "7"}
          ]
        """,
        stores.toSession()
    )

    @Test
    fun selectCorrelatedLeftJoinOnClause() = assertEval(
        """
        SELECT
          s.id AS id, b.title AS title
        FROM stores AS s LEFT OUTER JOIN @s.books AS b ON b.price > 9
        """,
        """
          [
            {id: "5"},
            {id: "6", title: "E"},
            {id: "6", title: "F"},
            {id: "7"}
          ]
        """,
        stores.toSession()
    )

    @Test
    fun selectJoinOnClauseScoping() = assertEval(
        // note that d is a global
        """
        SELECT VALUE [a, b, d]
        FROM
          [1, 3] AS a
        INNER JOIN [1, 2, 3] AS b ON b < d
        LEFT JOIN [1.1, 2.1] AS d ON b < d AND a <= d
        """,
        """
          [
            [1, 1, 1.1],
            [1, 1, 2.1],
            [1, 2, 2.1],
            [3, 1, null],
            [3, 2, null],
          ]
        """,
        (globalStruct + globalNumbers).toSession()
    )

    @Test
    fun selectNonCorrelatedJoin() = assertEval(
        // Note that the joined s is coming from the global scope without @-operator
        """SELECT s.id AS id, v AS title FROM stores AS s, s AS v""",
        """
          [
            {id: "5", title: "hello"},
            {id: "6", title: "hello"},
            {id: "7", title: "hello"},
          ]
        """,
        (stores + globalHello).toSession()
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
        """,
        globalStruct.toSession()
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
        """,
        (animals + animalTypes).toSession()
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
        """,
        (animals + animalTypes).toSession()
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
        """,
        animals.toSession()
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
        """,
        animals.toSession()
    )

    @Test
    fun pivotBadFieldType() = assertEval(
        """
          PIVOT a.name AT i FROM animals AS a AT i
        """,
        """
          {}
        """,
        animals.toSession()
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
          SELECT s.id, s.books[1].title FROM stores AS s WHERE s.id = '5'
        """,
        """
          [
            {
              id: "5",
              title: "B"
            }
          ]
        """,
        stores.toSession()
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
        """,
        stores.toSession()
    )

    @Test
    fun inPredicateSingleItem() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price IN (5)
        """,
        """
          [
            "A", "A"
          ]
        """,
        stores.toSession()
    )

    @Test
    fun inPredicateSingleExpr() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price IN 5
        """,
        """
          []
        """,
        stores.toSession()
    )

    @Test
    fun inPredicateSingleItemListVar() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price IN (prices)
        """,
        """
          []
        """,
        storesAndPrices.toSession()
    )

    @Test
    fun inPredicateSingleListVar() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price IN prices
        """,
        """
          [
            "A", "B", "A"
          ]
        """,
        storesAndPrices.toSession()
    )

    @Test
    fun inPredicateSubQuerySelectValue() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price IN (SELECT VALUE p FROM prices AS p)
        """,
        """
          [
            "A", "B", "A"
          ]
        """,
        storesAndPrices.toSession()
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
        """,
        stores.toSession()
    )

    @Test
    fun notInPredicateSingleItem() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price NOT IN (5)
        """,
        """
          [
            "B", "C", "D", "E", "F"
          ]
        """,
        stores.toSession()
    )

    @Test
    fun notInPredicateSingleExpr() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price NOT IN 5
        """,
        """
          [
            "A", "B", "C", "D", "A", "E", "F"
          ]
        """,
        stores.toSession()
    )

    @Test
    fun notInPredicateSingleItemListVar() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price NOT IN (prices)
        """,
        """
          [
            "A", "B", "C", "D", "A", "E", "F"
          ]
        """,
        storesAndPrices.toSession()
    )

    @Test
    fun notInPredicateSingleListVar() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price NOT IN prices
        """,
        """
          [
            "C", "D", "E", "F"
          ]
        """,
        storesAndPrices.toSession()
    )

    @Test
    fun notInPredicateSubQuerySelectValue() = assertEval(
        """
          SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price NOT IN (SELECT VALUE p FROM prices AS p)
        """,
        """
          [
            "C", "D", "E", "F"
          ]
        """,
        storesAndPrices.toSession()
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
        """,
        stores.toSession()
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
        """,
        stores.toSession()
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
        """,
        stores.toSession()
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
        """,
        stores.toSession()
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
        """,
        globalNumbers.toSession()
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
        """,
        globalNumbers.toSession()
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
        """,
        globalNumbers.toSession()
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
        """,
        globalNumbers.toSession()
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
        """,
        globalNumbers.toSession()
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
        """,
        globalNumbers.toSession()
    )

    @Test
    fun betweenStringsPredicate() = assertEval(
        """
          SELECT VALUE x
          FROM << 'APPLE', 'AZURE', 'B', 'XZ', 'ZOE', 'YOYO' >> AS x
          WHERE x BETWEEN 'B' AND 'Y'
        """,
        """
          [
            "B", "XZ"
          ]
        """
    )

    @Test
    fun notBetweenStringsPredicate() = assertEval(
        """
          SELECT VALUE x
          FROM << 'APPLE', 'AZURE', 'B', 'XZ', 'Z', 'ZOE', 'YOYO' >> AS x
          WHERE x NOT BETWEEN 'B' AND 'Y'
        """,
        """
          [
            "APPLE", "AZURE", "Z", "ZOE", "YOYO"
          ]
        """
    )

    @Test
    fun correlatedJoinWithShadowedAttributes() = assertEval(
        """SELECT VALUE v FROM `[{v:5}]` AS item, @item.v AS v""",
        """[5]"""
    )

    @Test
    fun correlatedJoinWithoutLexicalScope() = assertEval(
        """SELECT VALUE b FROM `[{b:5}]` AS item, item.b AS b""",
        """[5]"""
    )

    @Test
    fun joinWithShadowedGlobal() = assertEval(
        // 'a' is a global variable
        """SELECT VALUE b FROM `[{b:5}]` AS a, a.b AS b""",
        """[{c:{d:{e:5, f:6}}}]""",
        globalStruct.toSession()
    )

    @Test
    fun topLevelCountDistinct() = assertEval(
        """COUNT(DISTINCT [1,1,1,1,2])""",
        """2"""
    )

    @Test
    fun topLevelCount() = assertEval(
        """COUNT(numbers)""",
        """5""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun topLevelAllCount() = assertEval(
        """COUNT(ALL numbers)""",
        """5""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun topLevelSum() = assertEval(
        """SUM(numbers)""",
        """15.0""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun topLevelAllSum() = assertEval(
        """SUM(ALL numbers)""",
        """15.0""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun topLevelDistinctSum() = assertEval(
        """SUM(DISTINCT [1,1,1,1,1,1,1,2])""",
        """3"""
    )

    @Test
    fun topLevelMin() = assertEval(
        """MIN(numbers)""",
        """1""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun topLevelDistinctMin() = assertEval(
        """MIN(DISTINCT numbers)""",
        """1""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun topLevelAllMin() = assertEval(
        """MIN(ALL numbers)""",
        """1""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun topLevelMax() = assertEval(
        """MAX(numbers)""",
        """5d0""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun topLevelDistinctMax() = assertEval(
        """MAX(DISTINCT numbers)""",
        """5d0""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun topLevelAllMax() = assertEval(
        """MAX(ALL numbers)""",
        """5d0""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun topLevelAvg() = assertEval(
        """AVG(numbers)""",
        """3.0""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun topLevelDistinctAvg() = assertEval(
        """AVG(DISTINCT [1,1,1,1,1,3])""",
        """2."""
    )

    @Test // AVG of integers should be of type DECIMAL.
    fun topLevelAvgOnlyInt() = assertEval(
        """AVG([2,2,2,4])""",
        """2.5""")

    @Test
    fun selectValueAggregate() = assertEval(
        // SELECT VALUE does not do legacy aggregation
        """SELECT VALUE COUNT(v) + SUM(v) FROM <<numbers, numbers>> AS v""",
        """[20.0, 20.0]""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun selectListCountStar() = assertEval(
        """SELECT COUNT(*) AS c FROM <<numbers, numbers>> AS v""",
        """[{c:2}]""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun selectListCountVariable() = assertEval(
        """SELECT COUNT(v) AS c FROM <<numbers, numbers>> AS v""",
        """[{c:2}]""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun selectListMultipleAggregates() = assertEval(
        """SELECT COUNT(*) AS c, AVG(v * 2) + SUM(v + v) AS result FROM numbers AS v""",
        """[{c:5, result:36.0}]""",
        globalListOfNumbers.toSession()
    )

    @Test
    fun selectListMultipleAggregatesNestedQuery() = assertEval(
        """
            SELECT VALUE
              (SELECT MAX(v2 * v2) + MIN(v2 * 2) * v1 AS result FROM numbers AS v2)
            FROM numbers AS v1
        """,
            """[
            [{result:27.}],
            [{result:29.0}],
            [{result:31.}],
            [{result:33.}],
            [{result:35.}],
        ]""",
            globalListOfNumbers.toSession()
        )

    @Test
    fun selectListWithMissing() = assertEval(
        """SELECT a.x AS x, a.y AS y FROM `[{x:5}, {y:6}]` AS a""",
        """[{x:5}, {y:6}]"""
    )

    @Test
    fun selectValueStructConstructorWithMissing() = assertEval(
        """SELECT VALUE {'x': a.x, 'y': a.y} FROM `[{x:5}, {y:6}]` AS a""",
        """[{x:5}, {y:6}]"""
    )

    @Test
    fun undefinedUnqualifiedVariableWithUndefinedVariableBehaviorMissing() {
        assertEvalIsMissing("undefined_variable",
                   compileOptions = CompileOptions.build { undefinedVariable(UndefinedVariableBehavior.MISSING) })
    }

    @Test
    fun undefinedUnqualifiedVariableIsNullExprWithUndefinedVariableBehaviorMissing() {
        assertEval("undefined_variable IS NULL", "true",
                   compileOptions = CompileOptions.build { undefinedVariable(UndefinedVariableBehavior.MISSING) })
    }
    @Test
    fun undefinedUnqualifiedVariableIsMissingExprWithUndefinedVariableBehaviorMissing() {
        assertEval("undefined_variable IS MISSING", "true",
                   compileOptions = CompileOptions.build { undefinedVariable(UndefinedVariableBehavior.MISSING) })
    }

    @Test
    fun undefinedUnqualifiedVariableInSelectWithUndefinedVariableBehaviorMissing() {
        val options = CompileOptions.build { undefinedVariable(UndefinedVariableBehavior.MISSING) }
        val query = "SELECT s.a, s.undefined_variable, s.b FROM `[{a:100, b:200}]` s"

        assertEval(query, "[{a:100, b:200}]", compileOptions = options)
        val actual = eval(query, compileOptions = options).iterator().next()
        assertEquals("100, 200", actual.iterator().asSequence().joinToString(separator = ", "))
    }

    @Test
    fun undefinedUnqualifiedVariable_inSelect_withProjectionOption() {
        val options = CompileOptions.build { projectionIteration(ProjectionIterationBehavior.UNFILTERED) }
        val query = "SELECT s.a, s.undefined_variable, s.b FROM `[{a:100, b:200}]` s"

        assertEval(query, "[{a:100, b:200}]", compileOptions = options)
        val actual = eval(query, compileOptions = options).iterator().next()
        assertEquals("100, MISSING, 200", actual.iterator().asSequence().joinToString(separator = ", "))
    }

    @Test
    fun projectionIterationBehaviorUnfiltered_select_list() =
        assertEvalExprValue(
            source = "select a from <<{'a': MISSING}>>",
            expected = "<<{'a': MISSING}>>",
            compileOptions = CompileOptions.build {
                projectionIteration(ProjectionIterationBehavior.UNFILTERED)
            }
        )

    @Test
    fun projectionIterationBehaviorUnfiltered_select_star() =
        assertEvalExprValue(
            source = "select * from <<{'a': MISSING}>>",
            expected = "<<{'a': MISSING}>>",
            compileOptions = CompileOptions.build {
                projectionIteration(ProjectionIterationBehavior.UNFILTERED)
            }
        )

    @Test
    fun undefinedQualifiedVariableWithUndefinedVariableBehaviorError() {
        // Demonstrates that UndefinedVariableBehavior.ERROR does not affect qualified field names.
        assertEval("SELECT t.a, t.undefined_field FROM `[{a:100, b:200}]` as t", "[{a:100}]",
                   compileOptions = CompileOptions.build { undefinedVariable(UndefinedVariableBehavior.ERROR) })
    }

    @Test
    fun ordinalAccessWithNegativeIndex() = assertEval("SELECT temp[-2] FROM <<[1,2,3,4]>> AS temp", "[{}]")

    @Test
    fun ordinalAccessWithNegativeIndexAndBindings()  = assertEval("SELECT temp[-2] FROM temp", "[{}]",
                                                                  mapOf("temp" to "[[1,2,3,4]]").toSession())

    @Test
    fun semicolonAtEndOfLiteral() = assertEval("1;", "1")

    @Test
    fun semicolonAtEndOfExpression() = assertEval("SELECT * FROM <<1>>;", "[{_1: 1}]")

    @Test
    fun emptySymbol() = assertEval(""" SELECT "" FROM `{'': 1}` """, "[{'': 1}]")

    @Test
    fun emptySymbolInGlobals() = assertEval(""" SELECT * FROM "" """, "[{_1: 1}]", mapOf("" to "1").toSession())


    @Test
    fun wildcardOrderedNames() {
        val result = eval("SELECT * FROM <<{'a': 1, 'b': 2 }>> AS f")
        assertNull("Ordering of the fields should not be known when '*' is used",
                   result.first().orderedNames)
    }

    @Test
    fun aliasWildcardOrderedNames() {
        val result = eval("SELECT f.* FROM <<{'a': 1, 'b': 2 }>> AS f")
        assertNull("Ordering of the fields should not be known when 'alias.*' is used",
                   result.first().orderedNames)
    }
    @Test
    fun aliasWildcardOrderedNamesSelectList() {
        val result = eval("SELECT f.a, f.* FROM <<{'a': 1, 'b': 2 }>> AS f")
        assertNull("Ordering of the fields should not be known when an 'alias.*' is used",
                   result.first().orderedNames)
    }

    @Test
    fun aliasOrderedNamesSelectList() {
        val result = eval("SELECT f.a, f.b FROM <<{'a': 1, 'b': 2 }>> AS f")
        assertEquals("Ordering of the fields should be known when no wildcards are used",
                     listOf("a", "b"),
                     result.first().orderedNames)
    }

    @Test
    fun selectDistinct() = assertEval(
        """SELECT DISTINCT t.a FROM `[{a: 1}, {a: 2}, {a: 1}]` t""",
        """
          [{a: 1}, {a: 2}]
        """)

    @Test
    fun selectDistinctWithAggregate() = assertEval(
        """SELECT SUM(DISTINCT t.a) AS a FROM `[{a:10}, {a:1}, {a:10}, {a:3}]` t""",
        "[{a:14}]")

    @Test
    fun selectDistinctSubQuery() = assertEval(
        """SELECT * FROM (SELECT DISTINCT t.a FROM `[{a: 1}, {a: 2}, {a: 1}]` t)""",
        """[{a:1},{a:2}]""")

    @Test
    fun selectDistinctWithSubQuery() = assertEval(
        """SELECT DISTINCT * FROM (SELECT t.a FROM `[{a: 1}, {a: 2}, {a: 1}]` t)""",
        """[{a:1},{a:2}]""")

    @Test
    fun selectDistinctAggregationWithGroupBy() = assertEval(
        """
            SELECT t.a, COUNT(DISTINCT t.b) AS c 
            FROM `[{a:1, b:10}, {a:1, b:10}, {a:1, b:20}, {a:2, b:10}, {a:2, b:10}]` t
            GROUP by t.a
        """,
        """[{a:1, c:2}, {a:2, c:1}]""")

    @Test
    fun selectDistinctWithGroupBy() = assertEval(
        """
            SELECT DISTINCT t.a, COUNT(t.b) AS c 
            FROM `[{a:1, b:10}, {a:1, b:10}, {a:1, b:20}, {a:2, b:10}, {a:2, b:10}]` t
            GROUP by t.a
        """,
        """[{a:1, c:3}, {a:2, c:2}]""")

    @Test
    fun selectDistinctWithJoin() = assertEval(
        """
            SELECT DISTINCT *  
            FROM 
                `[1, 1, 1, 1, 2]` t1,
                `[2, 2, 2, 2, 1]` t2
        """,
        """
          [{_1:1,_2:2}, {_1:1, _2:1}, {_1:2,_2:2}, {_1:2,_2:1}]
        """)

    @Test
    @Ignore("https://github.com/partiql/partiql-lang-kotlin/issues/169")
    fun selectDistinctStarMixed() = assertEval(
        """
            SELECT DISTINCT * 
            FROM [
                1, 1, 2, 
                [1], [1], [1, 2],
                <<>>, <<>>,
                MISSING, NULL, NULL, MISSING, 
                {'a':1}, {'a':1}, {'a':2}]
        """,
        """
          [{_1:1},{_1:2},{},{_1:null},{a:1},{a:2}]
        """)

    @Test
    fun selectDistinctStarScalars() = assertEval(
        """
            SELECT DISTINCT * FROM [1, 1, 2]
        """,
        """
          [{_1:1},{_1:2}]
        """)

    @Test
    fun selectDistinctStarStructs() = assertEval(
        """
            SELECT DISTINCT * FROM [ {'a':1}, {'a':1}, {'a':2} ]
        """,
        """
          [{a:1},{a:2}]
        """)

    @Test
    fun selectDistinctStarUnknowns() = assertEval(
        """
            SELECT DISTINCT * FROM [MISSING, NULL, NULL, MISSING]
        """,
        """
          [{}, {_1: null}]
        """)

    @Test
    @Ignore("https://github.com/partiql/partiql-lang-kotlin/issues/169")
    fun selectDistinctStarBags() = assertEval(
        """
            SELECT DISTINCT * FROM [ <<>>, <<>>, <<1>>, <<1>>, <<1, 2>>, <<2, 1>>, <<3, 4>>]
        """,
        """
            [{_1:[]}, {_1: [1]}]
        """)

    @Test
    @Ignore("https://github.com/partiql/partiql-lang-kotlin/issues/169")
    fun selectDistinctStarLists() = assertEval(
        """
            SELECT DISTINCT * FROM [[1], [1], [1, 2]]
        """,
        """
            [{_1:[1]}, {_1: [1, 2]}]
        """)

    @Test
    fun selectDistinctStarIntegers() = assertEval(
        """
            SELECT DISTINCT * FROM [ 1, 1, 2 ]
        """,
        """
          [{_1:1},{_1:2}]
        """)

    @Test
    fun selectDistinctValue() = assertEval(
        """
            SELECT DISTINCT VALUE t 
            FROM [1,2,3,1,1,1,1,1] t
                
        """,
        """
          [1,2,3]
        """)

    @Test
    fun selectDistinctExpressionAndWhere() = assertEval(
        """
            SELECT DISTINCT (t.a + t.b) as c 
            FROM `[{a: 1, b: 1}, {a: 2, b: 0}, {a: 0, b: 2}, {a: 2, b: 2}, {a: 0, b: 99}]` t
            WHERE t.a > 0
        """,
        """
          [{c: 2}, {c: 4}]
        """)

    @Test
    fun selectDistinctExpression() = assertEval(
        """
            SELECT DISTINCT (t.a || t.b) as c 
            FROM `[{a: "1", b: "1"}, {a: "11", b: ""}, {a: "", b: "11"}, {a: "2", b: "2"}]` t
                
        """,
        """
          [{c:"11"},{c:"22"}]
        """)

    @Test
    fun projectOfListOfList() = assertEvalExprValue("SELECT * FROM [ [1,2] ] as foo", "<<{'_1': [1,2] }>>")

    @Test
    fun projectOfBagOfBag() = assertEvalExprValue("SELECT * FROM << <<1,2>> >> as foo", "<<{'_1': <<1,2>> }>>")

    @Test
    fun projectOfListOfBag() = assertEvalExprValue("SELECT * FROM [ <<1,2>> ] as foo", "<<{'_1': <<1,2>> }>>")

    @Test
    fun projectOfBagOfList() = assertEvalExprValue("SELECT * FROM << [1,2] >> as foo", "<<{'_1': [1,2] }>>")

    @Test
    fun projectOfSexp() = assertEvalExprValue("SELECT * FROM `(1 2)` as foo", "<<{'_1': `(1 2)` }>>")

    @Test
    fun projectOfUnpivotPath() = assertEvalExprValue("SELECT * FROM <<{'name': 'Marrowstone Brewing'}, {'name': 'Tesla'}>>.*",
        "<<{'_1': <<{'name': 'Marrowstone Brewing'}, {'name': 'Tesla'}>>}>>")

    /**
     * Regression test for https://github.com/partiql/partiql-lang-kotlin/issues/314
     *
     * Ensures that date parts can be used as variable names.
     */
    @Test
    fun datePartsAsVariableNames() =
        assertEvalExprValue(
            """
            SELECT VALUE [year, month, day, hour, minute, second]
            FROM 1968 AS year, 4 AS month, 3 as day, 12 as hour, 31 as minute, 59 as second 
            """,
            "<<[1968, 4, 3, 12, 31, 59]>>")

    /**
     * Regression test for https://github.com/partiql/partiql-lang-kotlin/issues/121
     *
     * Ensures that date parts can be used as struct field names.
     */
    @Test
    fun datePartsAsStructFieldNames() =
        assertEvalExprValue(
            """
            SELECT VALUE [x.year, x.month, x.day, x.hour, x.minute, x.second]
            FROM << { 'year': 1968, 'month': 4, 'day': 3, 'hour': 12, 'minute': 31, 'second': 59 }>> AS x
            """,
            "<<[1968, 4, 3, 12, 31, 59]>>")

    @Test
    fun aggregateInSubqueryOfSelect() =
        assertEvalExprValue(
            """
            SELECT foo.cnt
            FROM
                (SELECT COUNT(*) AS cnt 
                FROM [1, 2, 3])
            AS foo
            """,
            "<< { 'cnt': 3 } >>")

    @Test
    fun aggregateInSubqueryOfSelectValue() =
        assertEvalExprValue(
            """
            SELECT VALUE foo.cnt
            FROM
                (SELECT COUNT(*) AS cnt 
                FROM [1, 2, 3])
            AS foo
            """,
            "<< 3 >>")

    @Test
    fun aggregateWithAliasingInSubqueryOfSelectValue() =
        assertEvalExprValue(
            """
            SELECT VALUE foo.cnt
            FROM
                (SELECT COUNT(baz.bar) AS cnt 
                FROM << { 'bar': 1 }, { 'bar': 2 } >> AS baz)
            AS foo
            """,
            "<< 2 >>")

    @Test
    fun orderByThrowsCorrectException() {
        val ex = assertThrows<EvaluationException>("ORDER BY should throw unimplemented exception") {
            eval("SELECT 1 FROM <<>> ORDER BY x")
        }
        assertEquals(ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET, ex.errorCode)
        assertEquals("ORDER BY", ex.errorContext!![Property.FEATURE_NAME]!!.toString())
    }

}
