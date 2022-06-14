package org.partiql.lang.eval

import org.junit.Assert
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.partiql_bag
import org.partiql.lang.util.partiql_missing
import org.partiql.lang.util.testdsl.IonResultTestSuite
import org.partiql.lang.util.testdsl.defineTestSuite

class EvaluatorTestCasesAsExprNodeTestCases : ArgumentsProviderBase() {
    override fun getParameters() = EVALUATOR_TEST_SUITE.allTestsAsExprNodeTestCases()
}

/**
 * Defines test cases for the evaluator.
 *
 * Some of the tests must be executed with the globals defined by [org.partiql.lang.fakedata.EVALUATOR_TEST_GLOBALS].
 */
internal val EVALUATOR_TEST_SUITE: IonResultTestSuite = defineTestSuite {
    // construction of parameters must be deferred because we do not have an ExprValueFactory at the time the
    // test suite is defined.
    parameterFactory { vf: ExprValueFactory ->
        listOf("spam", "baz").map { vf.newString(it) }
    }

    // define global variables to be used in the test cases below.
    "parameterTestTable" hasVal """[{"bar": "baz"}, {"bar": "blargh"}]"""

    "" hasVal "1" // Note: the variable name here is intentionally empty!
    "a" hasVal "{b:{c:{d:{e:5, f:6}}}}"
    "b" hasVal "{ c: 100 }"
    "d" hasVal "3d0"
    "e" hasVal """[{books:["b1","b2"]}]"""
    // mappings hasValue different number types
    "f" hasVal "2e0"
    "i" hasVal "1"
    "l" hasVal """[{foobar: 1}]"""
    // Sample ion containing a collection of stores
    "numbers" hasVal "[1, 2.0, 3e0, 4, 5d0]"
    "someList" hasVal "[{a:1}, {a:2}, {a:3}]"
    "s" hasVal "\"hello\""
    "stores" hasVal """
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

    "prices" hasVal """[5, 2e0]"""

    "animals" hasVal """
        [
          {name: "Kumo", type: "dog"},
          {name: "Mochi", type: "dog"},
          {name: "Lilikoi", type: "unicorn"},
        ]
        """

    "animal_types" hasVal """
        [
          {id: "dog", is_magic: false},
          {id: "cat", is_magic: false},
          {id: "unicorn", is_magic: true},
        ]
        """

    "friends" hasVal """
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
    group("basic") {
        test("literal", "5", "5")
        test("identifier", "i", "1")
        test("identifierCaseMismatch", "I", "1")
        test("quotedIdentifier", "\"i\"", "1")
        test("lexicalScope", "@i", "1")
        test("functionCall", "exists(select * from [1])", "true")
        test("grouping", "((i))", "1")

        test("emptyListLiteral", "[]", "[]")
        test("listLiteral", "[i, f, d]", "[1, 2e0, 3d0]")
        test("rowValueConstructor", "(i, f, d)", "[1, 2e0, 3d0]")

        test("emptyBagLiteral", "<<>>", "$partiql_bag::[]")
        test("bagLiteral", "<<i, f, d>>", "$partiql_bag::[1, 2e0, 3d0]")
        test("tableValueConstructor", "VALUES (i), (f, d)", "$partiql_bag::[[1], [2e0, 3d0]]")

        test("emptyStructLiteral", "{}", "{}")
        test("structLiteral", "{'a':i, 'b':f, 'c':d, 'd': 1}", "{a:1, b:2e0, c:3d0, d:1}") { exprValue ->
            // struct literals provide ordered names
            val bindNames = exprValue.orderedNames!!
            Assert.assertEquals(listOf("a", "b", "c", "d"), bindNames)
        }

        test("decimalRoundUp", "1.9999999999999999999999999999999999999999999999", "2.0000000000000000000000000000000000000")
        test("decimalRoundDown", "1.00000000000000000000000000000000000000000001", "1.0000000000000000000000000000000000000")
    }
    group("nary") {
        // unary
        test("unaryPlus", "+i", "1")
        test("unaryMinus", "-f", "-2e0")

        // arithmetic
        test("binaryAddWith2Terms", "1 + 1", "2")
        test("binaryAddWith3Terms", "1 + 1 + 1", "3")
        test("addIntFloat", "i + f", "3e0")
        test("subIntFloatDecimal", "i - f - d", "-4.")
        test("repeatingDecimal", "4.0000 / 3.0", "1.3333333333333333333333333333333333333")
        test("repeatingDecimalHigherPrecision", "4.000000000000000000000000000000000000/3.0", "1.3333333333333333333333333333333333333")
        test("divDecimalInt", "d / 2", "1.5")
        test("subtractionOutOfAllowedPrecision", "1e100 - 1e-100", "10000000000000000000000000000000000000d63")
        test("bigDecimals", "${Long.MAX_VALUE}.0 + 100.0", "9223372036854775907.0")
        test("mulFloatIntInt", "f * 2 * 4", "16e0")
        test("modIntInt", "3 % 2", "1")

        // equality
        test("equalIntFloat", "1 = 1e0", "true")
        test("equalIntFloatFalse", "1 = 1e1", "false")
        test("notEqualIntInt", "1 != 2", "true")
        test("notEqualIntFloat", "1 != `2e0`", "true")
        test("notEqualIntFloatFalse", "1 != `1e0`", "false")
        test("equalListDifferentTypesTrue", """[1, `2e0`, 'hello'] = [1.0, 2, `hello`]""", "true")
        test("equalListDifferentLengthsShortFirst", """[1.0, 2] = [1.0, 2, `hello`]""", "false")
        test("equalListDifferentLengthsLongFirst", """[1, `2e0`, 'hello'] = [1, `2e0`]""", "false")
        test("symbolEquality", """ 'A' = 'A' """, "true")
        test("symbolCaseEquality", """ 'A' = 'a' """, "false")

        // comparison
        test("moreIntFloat", "3 > `2e0`", "true")
        test("moreIntFloatFalse", "1 > `2e0`", "false")
        test("lessIntFloat", "1 < `2e0`", "true")
        test("lessIntFloatFalse", "3 < `2e0`", "false")
        test("moreEqIntFloat", "3 >= `2e0`", "true")
        test("moreEqIntFloatFalse", "1 >= `2e0`", "false")
        test("lessEqIntFloat", "1 <= `2e0`", "true")
        test("lessEqIntFloatFalse", "5 <= `2e0`", "false")
    }
    group("logical") {
        test("notTrue", "not true", "false")
        test("notFalse", "not false", "true")
        test("andTrueFalse", "true and false", "false")
        test("andTrueTrue", "true and true", "true")
        test("orTrueFalse", "true or false", "true")
        test("orFalseFalse", "false or false", "false")
        test("comparisonsConjuctTrue", "i < f and f < d", "true")
        test("comparisonsDisjunctFalse", "i < f and (f > d or i > d)", "false")
    }
    group("path") {
        test("pathSimpleDotOnly", "b.c", "100")
        test("pathDotOnly", "a.b.c.d.e", "5")
        test("pathDotMissingAttribute", "a.z IS MISSING", "true")
        test("pathMissingDotName", "(MISSING).a IS MISSING", "true")
        test("pathNullDotName", "(NULL).a IS MISSING", "true")
        test("pathIndexing", "stores[0].books[2].title", "\"C\"")
        test("pathIndexListLiteral", "[1, 2, 3][1]", "2")
        test("pathIndexBagLiteral", "<<1, 2, 3>>[1]", "\$partiql_missing::null")
        test("pathFieldStructLiteral", "{'a': 1, 'b': 2, 'b': 3}.a", "1")
        test("pathIndexStructLiteral", "{'a': 1, 'b': 2, 'b': 3}[1]", "2")
        test("pathIndexStructOutOfBoundsLowLiteral", "{'a': 1, 'b': 2, 'b': 3}[-1]", "\$partiql_missing::null")
        test("pathIndexStructOutOfBoundsHighLiteral", "{'a': 1, 'b': 2, 'b': 3}[3]", "\$partiql_missing::null")
        test("pathUnpivotWildcard", "friends.kumo.likes.*", """$partiql_bag::[{type:"dog"},{type:"human"}]""")
        test("pathUnpivotWildcardFieldsAfter", "friends.kumo.likes.*.type", """$partiql_bag::["dog", "human"]""")
        test("pathSimpleWildcard", "someList[*].a", """$partiql_bag::[1, 2, 3]""")
        test("selectValuePath", "SELECT VALUE v1.books FROM e AS v1", """$partiql_bag::[["b1", "b2"]]""")
        test("pathWildcardPath", "e[*].books", """$partiql_bag::[["b1", "b2"]]""")
        test("pathWildcard", "stores[0].books[*].title", """$partiql_bag::["A", "B", "C", "D"]""")
        test("pathDoubleWildCard", "stores[*].books[*].title", """$partiql_bag::["A", "B", "C", "D", "A", "E", "F"]""")
        test("pathDoubleUnpivotWildCard", "friends.*.likes.*.type", """$partiql_bag::["dog", "human", "dog", "cat"]""")
        test("pathWildCardOverScalar", "s[*]", """$partiql_bag::["hello"]""")
        test("pathUnpivotWildCardOverScalar", "s.*", """$partiql_bag::["hello"]""")
        test("pathWildCardOverScalarMultiple", "(100)[*][*][*]", """$partiql_bag::[100]""")
        test("pathUnpivotWildCardOverScalarMultiple", "(100).*.*.*", """$partiql_bag::[100]""")
        test("pathWildCardOverStructMultiple", "a[*][*][*][*]", """$partiql_bag::[{b:{c:{d:{e:5, f:6}}}}]""")
        test("pathUnpivotWildCardOverStructMultiple", "a.*.*.*.*", """$partiql_bag::[5, 6]""")
    }

    val undefinedVariableMisisng: CompileOptions.Builder.() -> Unit = {
        undefinedVariable(UndefinedVariableBehavior.MISSING)
    }

    group("undefined_variable_behavior") {
        test(
            "undefinedUnqualifiedVariableWithUndefinedVariableBehaviorMissing",
            "undefined_variable",
            "$partiql_missing::null",
            compileOptionsBuilderBlock = undefinedVariableMisisng
        )

        test(
            "undefinedUnqualifiedVariableIsNullExprWithUndefinedVariableBehaviorMissing",
            "undefined_variable IS NULL",
            "true",
            compileOptionsBuilderBlock = undefinedVariableMisisng
        )

        test(
            "undefinedUnqualifiedVariableIsMissingExprWithUndefinedVariableBehaviorMissing",
            "undefined_variable IS MISSING",
            "true",
            compileOptionsBuilderBlock = undefinedVariableMisisng
        )

        test(
            "undefinedUnqualifiedVariableInSelectWithUndefinedVariableBehaviorMissing",
            "SELECT s.a, s.undefined_variable, s.b FROM `[{a:100, b:200}]` s",
            "$partiql_bag::[{a:100, b:200}]",
            compileOptionsBuilderBlock = undefinedVariableMisisng
        )
    }

    group("path in from clause") {
        test(
            "selectFromScalarAndAtUnpivotWildCardOverScalar",
            "SELECT VALUE [n, v] FROM (100).* AS v AT n",
            """$partiql_bag::[ ["_1", 100] ]"""
        )

        test(
            "selectFromListAndAtUnpivotWildCardOverScalar",
            "SELECT VALUE [n, (SELECT VALUE [i, x] FROM @v AS x AT i)] FROM [100, 200].*.*.* AS v AT n",
            """$partiql_bag::[ ["_1", $partiql_bag::[[0, 100], [1, 200]]] ]"""
        )

        test(
            "selectFromBagAndAtUnpivotWildCardOverScalar",
            """ SELECT VALUE [n, (SELECT VALUE [i IS MISSING, i, x] FROM @v AS x AT i)] FROM <<100, 200>>.* AS v AT n """,
            """$partiql_bag::[["_1",$partiql_bag::[[true,$partiql_missing::null,100],[true,$partiql_missing::null,200]]]]"""
        )

        test(
            "selectPathUnpivotWildCardOverStructMultiple",
            "SELECT name, val FROM a.*.*.*.* AS val AT name",
            """$partiql_bag::[{name: "e", val: 5}, {name: "f", val: 6}]"""
        )

        test(
            "selectStarSingleSourceHoisted",
            "SELECT * FROM stores[*].books[*] AS b WHERE b.price >= 9.0",
            """
                $partiql_bag::[
                    {title:"D", price: 9.0, categories:["suspense"]},
                    {title:"E", price: 9.5, categories:["fantasy", "comedy"]},
                    {title:"F", price: 10.0, categories:["history"]},
                ]
            """
        )
        test("ordinalAccessWithNegativeIndex", "SELECT temp[-2] FROM <<[1,2,3,4]>> AS temp", "$partiql_bag::[{}]")
        test(
            "ordinalAccessWithNegativeIndexAndBindings",
            "SELECT temp[-2] FROM [[1,2,3,4]] AS temp",
            "$partiql_bag::[{}]"
        )
    }

    group("various types in from clause") {
        test("rangeOverScalar", "SELECT VALUE v FROM 1 AS v", "$partiql_bag::[1]")
        test("rangeTwiceOverScalar", "SELECT VALUE [v1, v2] FROM 1 AS v1, @v1 AS v2", "$partiql_bag::[[1, 1]]")
        test("rangeOverSexp", "SELECT VALUE v FROM `(a b c)` AS v", "$partiql_bag::[(a b c)]")
        test("rangeOverStruct", "SELECT VALUE v FROM `{a:5}` AS v", "$partiql_bag::[{a:5}]")
        test("rangeOverList", "SELECT VALUE v FROM `[1, 2, 3]` AS v", "$partiql_bag::[1, 2, 3]")
        test("rangeOverListWithAt", "SELECT VALUE i FROM `[1, 2, 3]` AT i", "$partiql_bag::[0, 1, 2]")
        test("rangeOverListWithAsAndAt", "SELECT VALUE [i, v] FROM `[1, 2, 3]` AS v AT i", "$partiql_bag::[[0, 1], [1, 2], [2, 3]]")
        test("rangeOverListConstructorWithAt", "SELECT VALUE i FROM [1, 2, 3] AT i", """$partiql_bag::[0, 1, 2]""")
        test("rangeOverListConstructorWithAsAndAt", "SELECT VALUE [i, v] FROM [1, 2, 3] AS v AT i", "$partiql_bag::[[0, 1], [1, 2], [2, 3]]")
        test(
            "rangeOverBagWithAt",
            "SELECT VALUE [i, v] FROM <<1, 2, 3>> AS v AT i",
            "$partiql_bag::[[$partiql_missing::null, 1], [$partiql_missing::null, 2], [$partiql_missing::null, 3]]"
        )
        test(
            "rangeOverNestedWithAt",
            "SELECT VALUE [i, v] FROM (SELECT VALUE v FROM `[1, 2, 3]` AS v) AS v AT i",
            "$partiql_bag::[[$partiql_missing::null, 1], [$partiql_missing::null, 2], [$partiql_missing::null, 3]]"
        )
    }

    group("select list item") {
        test(
            "explicitAliasSelectSingleSource",
            "SELECT id AS name FROM stores",
            """$partiql_bag::[{name:"5"}, {name:"6"}, {name:"7"}]"""
        )

        test(
            "selectImplicitAndExplicitAliasSingleSourceHoisted",
            """SELECT title AS name, price FROM stores[*].books[*] AS b WHERE b.price >= 9.0""",
            """
              $partiql_bag::[
                {name:"D", price: 9.0},
                {name:"E", price: 9.5},
                {name:"F", price: 10.0},
              ]
            """
        )

        test(
            "syntheticColumnNameInSelect",
            """SELECT i+1 FROM <<100>> i""",
            """$partiql_bag::[{_1: 101}]"""
        )

        test(
            "properAliasFromPathInSelect",
            """
              SELECT s.id, s.books[1].title FROM stores AS s WHERE s.id = '5'
            """,
            """$partiql_bag::[ { id: "5", title: "B" } ] """
        )

        test(
            "selectListWithMissing",
            """SELECT a.x AS x, a.y AS y FROM `[{x:5}, {y:6}]` AS a""",
            """$partiql_bag::[{x:5}, {y:6}]"""
        )
    }

    group("joins") {
        test(
            "selectCrossProduct",
            """SELECT * FROM animals, animal_types""",
            """
              $partiql_bag::[
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
    }
    group("select_where") {
        test(
            "selectWhereStringEqualsSameCase",
            """SELECT * FROM animals as a WHERE a.name = 'Kumo' """,
            """
              $partiql_bag::[
                {name: "Kumo", type: "dog"}
              ]
            """
        )

        test(
            "selectWhereStrinEqualsDifferentCase",
            """SELECT * FROM animals as a WHERE a.name = 'KUMO' """,
            """
              $partiql_bag::[]
            """
        )
    }
    group("select_join") {
        test(
            "selectJoin",
            """SELECT * FROM animals AS a, animal_types AS t WHERE a.type = t.id""",
            """
              $partiql_bag::[
                {name: "Kumo", type: "dog", id: "dog", is_magic: false},
                {name: "Mochi", type: "dog", id: "dog", is_magic: false},
                {name: "Lilikoi", type: "unicorn", id: "unicorn", is_magic: true},
              ]
            """
        )

        test(
            "selectCorrelatedJoin",
            """SELECT s.id AS id, b.title AS title FROM stores AS s, @s.books AS b WHERE b IS NULL OR b.price > 5""",
            """
              $partiql_bag::[
                {id: "5", title: "C"},
                {id: "5", title: "D"},
                {id: "6", title: "E"},
                {id: "6", title: "F"},
              ]
            """
        )
        test(
            "selectCorrelatedLeftJoin",
            """SELECT s.id AS id, b.title AS title FROM stores AS s LEFT CROSS JOIN @s.books AS b WHERE b IS NULL""",
            """
              $partiql_bag::[
                {id: "7"}
              ]
            """
        )

        test(
            "selectCorrelatedLeftJoinOnClause",
            """
                SELECT
                  s.id AS id, b.title AS title
                FROM stores AS s LEFT OUTER JOIN @s.books AS b ON b.price > 9
            """,
            """
              $partiql_bag::[
                {id: "5"},
                {id: "6", title: "E"},
                {id: "6", title: "F"},
                {id: "7"}
              ]
            """
        )

        test(
            "selectJoinOnClauseScoping",
            // note that d is a global which should be shadowed by the last from source
            """
                SELECT VALUE [a, b, d]
                FROM [1, 3] AS a
                INNER JOIN [1, 2, 3] AS b ON b < d
                LEFT JOIN [1.1, 2.1] AS d ON b < d AND a <= d
            """,
            """
              $partiql_bag::[
                [1, 1, 1.1],
                [1, 1, 2.1],
                [1, 2, 2.1],
                [3, 1, null],
                [3, 2, null],
              ]
            """
        )

        test(
            "selectNonCorrelatedJoin",
            // Note that the joined s is coming from the global scope without @-operator
            "SELECT s.id AS id, v AS title FROM stores AS s, s AS v",
            """
              $partiql_bag::[
                {id: "5", title: "hello"},
                {id: "6", title: "hello"},
                {id: "7", title: "hello"},
              ]
            """
        )

        test(
            "selectCorrelatedUnpivot",
            """
              SELECT n1, n2, n3, n4, val
              FROM UNPIVOT a AS b AT n1,
                   UNPIVOT @b AS c AT n2,
                   UNPIVOT @c AS d AT n3,
                   UNPIVOT @d AS val AT n4
            """,
            """
              $partiql_bag::[
                {n1: "b", n2: "c", n3: "d", n4: "e", val: 5},
                {n1: "b", n2: "c", n3: "d", n4: "f", val: 6}
              ]
            """
        )

        test(
            "nestedSelectJoinWithUnpivot",
            """
              SELECT col, val
              FROM (SELECT * FROM animals AS aa, animal_types AS tt WHERE aa.type = tt.id) AS a,
                   UNPIVOT @a AS val AT col
              WHERE col != 'id'
            """,
            """
              $partiql_bag::[
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

        test(
            "nestedSelectJoinLimit",
            """
              SELECT col, val
              FROM (SELECT * FROM animals AS aa, animal_types AS tt WHERE aa.type = tt.id) AS a,
                   UNPIVOT @a AS val AT col
              WHERE col != 'id'
              LIMIT 6 - 3
            """,
            """
              $partiql_bag::[
                {col: "name", val: "Kumo"},
                {col: "type", val: "dog"},
                {col: "is_magic", val: false},
              ]
            """
        )
        test(
            "correlatedJoinWithShadowedAttributes",
            """SELECT VALUE v FROM `[{v:5}]` AS item, @item.v AS v""",
            """$partiql_bag::[5]"""
        )

        test(
            "correlatedJoinWithoutLexicalScope",
            """SELECT VALUE b FROM `[{b:5}]` AS item, item.b AS b""",
            """$partiql_bag::[5]"""
        )

        test(
            "joinWithShadowedGlobal",
            // 'a' is a global variable
            """SELECT VALUE b FROM `[{b:5}]` AS a, a.b AS b""",
            """$partiql_bag::[{c:{d:{e:5, f:6}}}]"""
        )
    }
    group("pivot") {
        test(
            "pivotFrom",
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

        test(
            "pivotLiteralFieldNameFrom",
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

        test(
            "pivotBadFieldType",
            """
              PIVOT a.name AT i FROM animals AS a AT i
            """,
            """
              {}
            """
        )

        test(
            "pivotUnpivotWithWhereLimit",
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
    }

    group("in") {
        test(
            "inPredicate",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price IN (5, `2e0`)
            """,
            """$partiql_bag::["A", "B", "A"]"""
        )

        test(
            "inPredicateSingleItem",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price IN (5)
            """,
            """$partiql_bag::[ "A", "A" ]"""
        )

        test(
            "inPredicateSingleExpr",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price IN 5
            """,
            "$partiql_bag::[]"
        )

        test(
            "inPredicateSingleItemListVar",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price IN (prices)
            """,
            """$partiql_bag::[]"""
        )

        test(
            "inPredicateSingleListVar",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price IN prices
            """,
            """$partiql_bag::[ "A", "B", "A" ]"""
        )

        test(
            "inPredicateSubQuerySelectValue",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price IN (SELECT VALUE p FROM prices AS p)
            """,
            """$partiql_bag::[ "A", "B", "A" ]"""
        )

        test(
            "notInPredicate",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price NOT IN (5, `2e0`)
            """,
            """$partiql_bag::["C", "D", "E", "F" ] """
        )

        test(
            "notInPredicateSingleItem",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price NOT IN (5)
            """,
            """$partiql_bag::[ "B", "C", "D", "E", "F" ]"""
        )

        test(
            "notInPredicateSingleExpr",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price NOT IN 5
            """,
            // We assert the expected result below because it has a different result under [TypingMode.LEGACY].
            expectedLegacyModeIonResult = """$partiql_bag::[ "A", "B", "C", "D", "A", "E", "F"]""",
            expectedPermissiveModeIonResult = "$partiql_bag::[]"
        )
        test(
            "notInPredicateSingleItemListVar",
            """ SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price NOT IN (prices) """,
            """ $partiql_bag::[ "A", "B", "C", "D", "A", "E", "F" ] """
        )

        test(
            "notInPredicateSingleListVar",
            """ SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price NOT IN prices """,
            """ $partiql_bag::[ "C", "D", "E", "F" ] """
        )

        test(
            "notInPredicateSubQuerySelectValue",
            "SELECT VALUE b.title FROM stores[*].books[*] AS b WHERE b.price NOT IN (SELECT VALUE p FROM prices AS p)",
            """ $partiql_bag::[ "C", "D", "E", "F" ] """
        )

        test(
            "inPredicateWithTableConstructor",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b
              WHERE (b.title, b.price) IN (VALUES ('A', `5e0`), ('B', 3.0), ('X', 9.0))
            """,
            """ $partiql_bag::[ "A", "A" ] """
        )

        test(
            "notInPredicateWithTableConstructor",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b
              WHERE (b.title, b.price) NOT IN (VALUES ('A', `5e0`), ('B', 3.0), ('X', 9.0))
            """,
            """ $partiql_bag::[ "B", "C", "D", "E", "F" ] """
        )

        test(
            "inPredicateWithExpressionOnRightSide",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b
              WHERE 'comedy' IN b.categories
            """,
            """ $partiql_bag::[ "B", "E" ] """
        )

        test(
            "notInPredicateWithExpressionOnRightSide",
            """
              SELECT VALUE b.title FROM stores[*].books[*] AS b
              WHERE 'comedy' NOT IN b.categories
            """,
            """ $partiql_bag::[ "A", "C", "D", "A", "F" ] """
        )
    }
    group("case") {
        test(
            "simpleCase",
            """
              SELECT VALUE
                CASE x + 1
                  WHEN NULL THEN 'shouldnt be null'
                  WHEN NULL THEN 'shouldnt be missing'
                  WHEN i THEN 'ONE'
                  WHEN f THEN 'TWO'
                  WHEN d THEN 'THREE'
                  ELSE '?'
                END
              FROM << i, f, d, null, missing >> AS x
            """,
            """
              $partiql_bag::[ "TWO", "THREE", "?", "?", "?" ]
            """
        )

        test(
            "simpleCaseNoElse",
            """
              SELECT VALUE
                CASE x + 1
                  WHEN NULL THEN 'shouldnt be null'
                  WHEN NULL THEN 'shouldnt be missing'
                  WHEN i THEN 'ONE'
                  WHEN f THEN 'TWO'
                  WHEN d THEN 'THREE'
                END
              FROM << i, f, d, null, missing >> AS x
            """,
            """ $partiql_bag::[ "TWO", "THREE", null, null, null ] """
        )

        test(
            "searchedCase",
            """
              SELECT VALUE
                CASE
                  WHEN x + 1 < i THEN '< ONE'
                  WHEN x + 1 = f THEN 'TWO'
                  WHEN (x + 1 > d) AND (x + 1 < 100) THEN '>= THREE < 100'
                  ELSE '?'
                END
              FROM << -1.0000, i, f, d, 100e0, null, missing >> AS x
            """,
            """ $partiql_bag::[ "< ONE", "TWO", "?", ">= THREE < 100", "?", "?", "?" ] """
        )

        test(
            "searchedCaseNoElse",
            """
              SELECT VALUE
                CASE
                  WHEN x + 1 < i THEN '< ONE'
                  WHEN x + 1 = f THEN 'TWO'
                  WHEN (x + 1 > d) AND (x + 1 < 100) THEN '>= THREE < 100'
                END
              FROM << -1.0000, i, f, d, 100e0, null, missing >> AS x
            """,
            """ $partiql_bag::[ "< ONE", "TWO", null, ">= THREE < 100", null, null, null ] """
        )
    }
    group("between") {
        test(
            "betweenPredicate",
            """
              SELECT VALUE x
              FROM << -1.0000, i, f, d, 100e0 >> AS x
              WHERE x BETWEEN 1.000001 AND 3.0000000
            """,
            """ $partiql_bag::[ 2e0, 3d0 ] """
        )

        test(
            "notBetweenPredicate",
            """
              SELECT VALUE x
              FROM << -1.0000, i, f, d, 100e0 >> AS x
              WHERE x NOT BETWEEN 1.000001 AND 3.0000000
            """,
            """$partiql_bag::[ -1.0000, 1, 100d0 ] """
        )

        test(
            "betweenStringsPredicate",
            """
              SELECT VALUE x
              FROM << 'APPLE', 'AZURE', 'B', 'XZ', 'ZOE', 'YOYO' >> AS x
              WHERE x BETWEEN 'B' AND 'Y'
            """,
            """ $partiql_bag::[ "B", "XZ" ] """
        )

        test(
            "notBetweenStringsPredicate",
            """
              SELECT VALUE x
              FROM << 'APPLE', 'AZURE', 'B', 'XZ', 'Z', 'ZOE', 'YOYO' >> AS x
              WHERE x NOT BETWEEN 'B' AND 'Y'
            """,
            """ $partiql_bag::[ "APPLE", "AZURE", "Z", "ZOE", "YOYO" ] """
        )
    }
    group("aggregates") {
        test(
            "topLevelCountDistinct",
            """COUNT(DISTINCT [1,1,1,1,2])""",
            """2"""
        )

        test(
            "topLevelCount",
            """COUNT(numbers)""",
            """5"""
        )

        test(
            "topLevelAllCount",
            """COUNT(ALL numbers)""",
            """5"""
        )

        test(
            "topLevelSum",
            """SUM(numbers)""",
            """15.0"""
        )

        test(
            "topLevelAllSum",
            """SUM(ALL numbers)""",
            """15.0"""
        )

        test(
            "topLevelDistinctSum",
            """SUM(DISTINCT [1,1,1,1,1,1,1,2])""",
            """3"""
        )

        test(
            "topLevelMin",
            """MIN(numbers)""",
            """1"""
        )

        test(
            "topLevelDistinctMin",
            """MIN(DISTINCT numbers)""",
            """1"""
        )

        test(
            "topLevelAllMin",
            """MIN(ALL numbers)""",
            """1"""
        )

        test(
            "topLevelMax",
            """MAX(numbers)""",
            """5d0"""
        )

        test(
            "topLevelDistinctMax",
            """MAX(DISTINCT numbers)""",
            """5d0"""
        )

        test(
            "topLevelAllMax",
            """MAX(ALL numbers)""",
            """5d0"""
        )

        test(
            "topLevelAvg",
            """AVG(numbers)""",
            """3.0"""
        )

        test(
            "topLevelDistinctAvg",
            """AVG(DISTINCT [1,1,1,1,1,3])""",
            """2."""
        )

        // AVG of integers should be of type DECIMAL.
        test(
            "topLevelAvgOnlyInt",
            """AVG([2,2,2,4])""",
            """2.5"""
        )

        test(
            "selectValueAggregate",
            // SELECT VALUE does not do legacy aggregation
            """SELECT VALUE COUNT(v) + SUM(v) FROM <<numbers, numbers>> AS v""",
            """$partiql_bag::[20.0, 20.0]"""
        )

        test(
            "selectListCountStar",
            """SELECT COUNT(*) AS c FROM <<numbers, numbers>> AS v""",
            """$partiql_bag::[{c:2}]"""
        )

        test(
            "selectListCountVariable",
            """SELECT COUNT(v) AS c FROM <<numbers, numbers>> AS v""",
            """$partiql_bag::[{c:2}]"""
        )
        test(
            "selectListMultipleAggregates",
            """SELECT COUNT(*) AS c, AVG(v * 2) + SUM(v + v) AS result FROM numbers AS v""",
            "$partiql_bag::[{c:5, result:36.0}]"
        )

        test(
            "selectListMultipleAggregatesNestedQuery",
            """
            SELECT VALUE
              (SELECT MAX(v2 * v2) + MIN(v2 * 2) * v1 AS result FROM numbers AS v2)
            FROM numbers AS v1
            """,
            """$partiql_bag::[
                $partiql_bag::[{result:27.}],
                $partiql_bag::[{result:29.0}],
                $partiql_bag::[{result:31.}],
                $partiql_bag::[{result:33.}],
                $partiql_bag::[{result:35.}],
            ]"""
        )

        test(
            "aggregateInSubqueryOfSelect",
            """
            SELECT foo.cnt
            FROM
                (SELECT COUNT(*) AS cnt 
                FROM [1, 2, 3])
            AS foo
            """,
            "$partiql_bag::[{ 'cnt': 3 }]"
        )

        test(
            "aggregateInSubqueryOfSelectValue",
            """
            SELECT VALUE foo.cnt
            FROM
                (SELECT COUNT(*) AS cnt 
                FROM [1, 2, 3])
            AS foo
            """,
            "$partiql_bag::[3]"
        )

        test(
            "aggregateWithAliasingInSubqueryOfSelectValue",
            """
            SELECT VALUE foo.cnt
            FROM
                (SELECT COUNT(baz.bar) AS cnt 
                FROM << { 'bar': 1 }, { 'bar': 2 } >> AS baz)
            AS foo
            """,
            "$partiql_bag::[2]"
        )
    }
    group("projection iteration behavior unfiltered") {
        val projectionIterationUnfiltered: CompileOptions.Builder.() -> Unit = {
            projectionIteration(ProjectionIterationBehavior.UNFILTERED)
        }

        test(
            "undefinedUnqualifiedVariable_inSelect_withProjectionOption",
            "SELECT s.a, s.undefined_variable, s.b FROM `[{a:100, b:200}]` s",
            "$partiql_bag::[{a:100, b:200}]",
            compileOptionsBuilderBlock = projectionIterationUnfiltered
        ) { exprValue ->
            val actual = exprValue.iterator().next()
            Assert.assertEquals(actual.iterator().asSequence().joinToString(separator = ", "), "100, MISSING, 200")
        }

        test(
            "projectionIterationBehaviorUnfiltered_select_list",
            "select x.someColumn from <<{'someColumn': MISSING}>> AS x",
            "$partiql_bag::[{someColumn: $partiql_missing::null}]",
            compileOptionsBuilderBlock = projectionIterationUnfiltered
        )

        test(
            "projectionIterationBehaviorUnfiltered_select_star",
            "select * from <<{'someColumn': MISSING}>>",
            "$partiql_bag::[{someColumn: $partiql_missing::null}]",
            compileOptionsBuilderBlock = projectionIterationUnfiltered
        )
    }

    group("ordered names") {
        test(
            "wildcardOrderedNames",
            "SELECT * FROM <<{'a': 1, 'b': 2 }>> AS f",
            "$partiql_bag::[{a: 1, b: 2}]"
        ) { exprValue ->
            Assert.assertNull(
                "Ordering of the fields should not be known when '*' is used",
                exprValue.first().orderedNames
            )
        }

        test(
            "aliasWildcardOrderedNames",
            "SELECT f.* FROM <<{'a': 1, 'b': 2 }>> AS f",
            "$partiql_bag::[{a: 1, b: 2}]"
        ) { exprValue ->
            Assert.assertNull(
                "Ordering of the fields should not be known when 'alias.*' is used",
                exprValue.first().orderedNames
            )
        }

        test(
            "aliasWildcardOrderedNamesSelectList",
            "SELECT f.a, f.* FROM <<{'a': 1, 'b': 2 }>> AS f",
            "$partiql_bag::[{a: 1, a: 1, b: 2}]"
        ) { exprValue ->
            Assert.assertNull(
                "Ordering of the fields should not be known when an 'alias.*' is used",
                exprValue.first().orderedNames
            )
        }

        test(
            "aliasOrderedNamesSelectList",
            "SELECT f.a, f.b FROM <<{'a': 1, 'b': 2 }>> AS f",
            "$partiql_bag::[{a:1, b:2}]"
        ) { exprValue ->
            Assert.assertEquals(
                "Ordering of the fields should be known when no wildcards are used",
                listOf("a", "b"),
                exprValue.first().orderedNames
            )
        }
    }

    group("select distinct") {
        test(
            "selectDistinct",
            """SELECT DISTINCT t.a FROM `[{a: 1}, {a: 2}, {a: 1}]` t""",
            """$partiql_bag::[{a: 1}, {a: 2}] """
        )

        test(
            "selectDistinctWithAggregate",
            """SELECT SUM(DISTINCT t.a) AS a FROM `[{a:10}, {a:1}, {a:10}, {a:3}]` t""",
            "$partiql_bag::[{a:14}]"
        )

        test(
            "selectDistinctSubQuery",
            """SELECT * FROM (SELECT DISTINCT t.a FROM `[{a: 1}, {a: 2}, {a: 1}]` t)""",
            """$partiql_bag::[{a:1},{a:2}]"""
        )

        test(
            "selectDistinctWithSubQuery",
            """SELECT DISTINCT * FROM (SELECT t.a FROM `[{a: 1}, {a: 2}, {a: 1}]` t)""",
            """$partiql_bag::[{a:1},{a:2}]"""
        )

        test(
            "selectDistinctAggregationWithGroupBy",
            """
                SELECT t.a, COUNT(DISTINCT t.b) AS c 
                FROM `[{a:1, b:10}, {a:1, b:10}, {a:1, b:20}, {a:2, b:10}, {a:2, b:10}]` t
                GROUP by t.a
            """,
            """$partiql_bag::[{a:1, c:2}, {a:2, c:1}]"""
        )

        test(
            "selectDistinctWithGroupBy",
            """
                SELECT DISTINCT t.a, COUNT(t.b) AS c 
                FROM `[{a:1, b:10}, {a:1, b:10}, {a:1, b:20}, {a:2, b:10}, {a:2, b:10}]` t
                GROUP by t.a
            """,
            """$partiql_bag::[{a:1, c:3}, {a:2, c:2}]"""
        )

        test(
            "selectDistinctWithJoin",
            """
                SELECT DISTINCT *  
                FROM 
                    `[1, 1, 1, 1, 2]` t1,
                    `[2, 2, 2, 2, 1]` t2
            """,
            """$partiql_bag::[{_1:1,_2:2}, {_1:1, _2:1}, {_1:2,_2:2}, {_1:2,_2:1}]"""
        )

        test(
            "selectDistinctStarMixed",
            """
                SELECT DISTINCT * 
                FROM [
                    1, 1, 2, 
                    [1], [1], [1, 2],
                    <<>>, <<>>,
                    MISSING, NULL, NULL, MISSING, 
                    {'a':1}, {'a':1}, {'a':2}]
            """,
            "$partiql_bag::[{_1:1},{_1:2},{_1:[1]},{_1:[1,2]},{_1:$partiql_bag::[]},{},{_1:null},{a:1},{a:2}]"
        )

        test(
            "selectDistinctStarScalars",
            """ SELECT DISTINCT * FROM [1, 1, 2] """,
            """ $partiql_bag::[{_1:1},{_1:2}] """
        )

        test(
            "selectDistinctStarStructs",
            """ SELECT DISTINCT * FROM [ {'a':1}, {'a':1}, {'a':2} ] """,
            """ $partiql_bag::[{a:1},{a:2}] """
        )

        test(
            "selectDistinctStarUnknowns",
            "SELECT DISTINCT * FROM [MISSING, NULL, NULL, MISSING]",
            """ $partiql_bag::[{}, {_1: null}] """
        )

        test(
            "selectDistinctStarBags",
            "SELECT DISTINCT * FROM [ <<>>, <<>>, <<1>>, <<1>>, <<1, 2>>, <<2, 1>>, <<3, 4>>]",
            """
                $partiql_bag::[
                  {_1: $partiql_bag::[]},
                  {_1: $partiql_bag::[1]},
                  {_1: $partiql_bag::[1,2]},
                  {_1: $partiql_bag::[3,4]}
                ]
            """
        )

        test(
            "selectDistinctStarLists",
            "SELECT DISTINCT * FROM [[1], [1], [1, 2]]",
            "$partiql_bag::[{_1:[1]}, {_1: [1, 2]}]"
        )

        test(
            "selectDistinctStarIntegers",
            "SELECT DISTINCT * FROM [ 1, 1, 2 ]",
            "$partiql_bag::[{_1:1},{_1:2}]"
        )

        test(
            "selectDistinctValue",
            "SELECT DISTINCT VALUE t FROM [1,2,3,1,1,1,1,1] t",
            "$partiql_bag::[1,2,3]"
        )

        test(
            "selectDistinctExpressionAndWhere",
            """
                SELECT DISTINCT (t.a + t.b) as c 
                FROM `[{a: 1, b: 1}, {a: 2, b: 0}, {a: 0, b: 2}, {a: 2, b: 2}, {a: 0, b: 99}]` t
                WHERE t.a > 0
            """,
            """
              $partiql_bag::[{c: 2}, {c: 4}]
            """
        )

        test(
            "selectDistinctExpression",
            """
            SELECT DISTINCT (t.a || t.b) as c 
            FROM `[{a: "1", b: "1"}, {a: "11", b: ""}, {a: "", b: "11"}, {a: "2", b: "2"}]` t
            """,
            """
              $partiql_bag::[{c:"11"},{c:"22"}]
            """
        )
    }
    group("project various container types") {

        test("projectOfListOfList", "SELECT * FROM [ [1,2] ] as foo", "$partiql_bag::[{_1: [1,2] }]")

        test("projectOfBagOfBag", "SELECT * FROM << <<1,2>> >> as foo", "$partiql_bag::[{_1: $partiql_bag::[1,2] }]")

        test("projectOfListOfBag", "SELECT * FROM [ <<1,2>> ] as foo", "$partiql_bag::[{_1: $partiql_bag::[1,2] }]")

        test("projectOfBagOfList", "SELECT * FROM << [1,2] >> as foo", "$partiql_bag::[{_1: [1,2] }]")

        test("projectOfSexp", "SELECT * FROM `(1 2)` as foo", "$partiql_bag::[{_1: (1 2) }]")

        test(
            "projectOfUnpivotPath", "SELECT * FROM <<{'name': 'Marrowstone Brewing'}, {'name': 'Tesla'}>>.*",
            """$partiql_bag::[{_1: $partiql_bag::[{name: "Marrowstone Brewing"}, {name: "Tesla"}]}]"""
        )
    }

    group("misc") {
        test(
            "parameters",
            """SELECT ? as b1, f.bar FROM parameterTestTable f WHERE f.bar = ?""",
            """$partiql_bag::[{b1:"spam",bar:"baz"}]"""
        )
    }

    group("floatN") {
        // These test cases ensure we ignore the type parameter of FLOAT in [TypedOpBehavior.LEGACY] mode.
        test("castAsFloat1", "CAST(1.234 AS FLOAT(1))", "1.234e0")
        test("canCastAsFloat1", "CAN_CAST(1.234 AS FLOAT(1))", "true")
        test("isFloat1", "`1.234e0` IS FLOAT(1)", "true")
    }

    group("pathUnpivotMissing") {
        test(
            "unpivotMissing",
            "SELECT * FROM UNPIVOT MISSING",
            "$partiql_bag::[]"
        )

        test(
            "unpivotEmptyStruct",
            "SELECT * FROM UNPIVOT {}",
            "$partiql_bag::[]"
        )

        test(
            "unpivotStructWithMissingField",
            "SELECT * FROM UNPIVOT { 'a': MISSING }",
            "$partiql_bag::[]"
        )

        test(
            "unpivotMissingWithAsAndAt",
            "SELECT unnestIndex, unnestValue FROM UNPIVOT MISSING AS unnestValue AT unnestIndex",
            "$partiql_bag::[]"
        )

        test(
            "unpivotMissingCrossJoinWithAsAndAt",
            "SELECT unnestIndex, unnestValue FROM MISSING, UNPIVOT MISSING AS unnestValue AT unnestIndex",
            "$partiql_bag::[]"
        )

        // double unpivots with wildcard paths
        test(
            "pathUnpivotEmptyStruct1",
            "{}.*.*.bar",
            "$partiql_bag::[]"
        )

        test(
            "pathUnpivotEmptyStruct2",
            "{}.*.bar.*",
            "$partiql_bag::[]"
        )

        test(
            "pathUnpivotEmptyStruct3",
            "{}.*.bar.*.baz",
            "$partiql_bag::[]"
        )
    }

    group("uncategorized") {
        test(
            "variableShadow",
            // Note that i, f, d, and s are defined in the global environment
            "SELECT f, d, s FROM i AS f, f AS d, @f AS s WHERE f = 1 AND d = 2e0 and s = 1",
            "$partiql_bag::[{f: 1, d: 2e0, s: 1}]"
        )

        test(
            "selectValueStructConstructorWithMissing",
            """SELECT VALUE {'x': a.x, 'y': a.y} FROM `[{x:5}, {y:6}]` AS a""",
            """$partiql_bag::[{x:5}, {y:6}]"""
        )

        test(
            "selectIndexStruct",
            "SELECT VALUE x[0] FROM (SELECT s.id FROM stores AS s) AS x",
            """$partiql_bag::["5", "6", "7"]"""
        )

        test(
            "selectStarSingleSource",
            "SELECT * FROM animals",
            """
                    $partiql_bag::[
                        {name: "Kumo", type: "dog"},
                        {name: "Mochi", type: "dog"},
                        {name: "Lilikoi", type: "unicorn"},
                    ]
                """
        )
        test(
            "implicitAliasSelectSingleSource",
            "SELECT id FROM stores",
            """$partiql_bag::[{id:"5"}, {id:"6"}, {id:"7"}]"""
        ) { exprValue ->
            // SELECT list provides ordered names facet
            exprValue.forEach {
                val bindNames = it.orderedNames!!
                Assert.assertEquals(listOf("id"), bindNames)
            }
        }

        test("selectValues", "SELECT VALUE id FROM stores", """$partiql_bag::["5", "6", "7"]""")

        test(
            "explicitAliasSelectSingleSourceWithWhere",
            """SELECT id AS name FROM stores WHERE id = '5' """,
            """$partiql_bag::[{name:"5"}]"""
        )

        // Demonstrates that UndefinedVariableBehavior.ERROR does not affect qualified field names.
        test(
            "undefinedQualifiedVariableWithUndefinedVariableBehaviorError",
            "SELECT t.a, t.undefined_field FROM `[{a:100, b:200}]` as t",
            "$partiql_bag::[{a:100}]"
        )

        test("emptySymbol", """ SELECT "" FROM `{'': 1}` """, "$partiql_bag::[{'': 1}]")
        test("emptySymbolInGlobals", """ SELECT * FROM "" """, "$partiql_bag::[{_1: 1}]")

        test("semicolonAtEndOfLiteral", "1;", "1")
        test("semicolonAtEndOfExpression", "SELECT * FROM <<1>>;", "$partiql_bag::[{_1: 1}]")
    }
    group("regression") {
        // https://github.com/partiql/partiql-lang-kotlin/issues/314
        // Ensures that datetime parts can be used as variable names.
        test(
            "dateTimePartsAsVariableNames",
            """
            SELECT VALUE [year, month, day, hour, minute, second]
            FROM 1968 AS year, 4 AS month, 3 as day, 12 as hour, 31 as minute, 59 as second 
            """,
            "$partiql_bag::[[1968, 4, 3, 12, 31, 59]]"
        )
        // https://github.com/partiql/partiql-lang-kotlin/issues/314
        // Ensures that datetime parts can be used as variable names.
        test(
            "dateTimePartsAsStructFieldNames",
            """
            SELECT VALUE [x.year, x.month, x.day, x.hour, x.minute, x.second]
            FROM << { 'year': 1968, 'month': 4, 'day': 3, 'hour': 12, 'minute': 31, 'second': 59 }>> AS x
            """,
            "$partiql_bag::[[1968, 4, 3, 12, 31, 59]]"
        )
    }
}
