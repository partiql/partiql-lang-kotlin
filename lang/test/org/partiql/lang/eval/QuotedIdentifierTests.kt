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

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property

class QuotedIdentifierTests : EvaluatorTestBase() {

    private val simpleSession = mapOf(
        "Abc" to "1",
        "aBc" to "2",
        "abC" to "3"
    ).toSession()

    private val sessionWithCaseVaryingTables = mapOf(
        "Abc" to "[{n:1}]",
        "aBc" to "[{n:2}]",
        "abC" to "[{n:3}]"
    ).toSession()

    private val simpleSessionWithTables = mapOf(
        "a" to "[{n:1}]",
        "b" to "[{n:2}]",
        "c" to "[{n:3}]",
        "" to "empty_variable_name_value"
    ).toSession()

    private val undefinedVariableMissingCompileOptions = CompileOptions.build { undefinedVariable(UndefinedVariableBehavior.MISSING) }

    @Test
    fun quotedIdResolvesWithSensitiveCase() {

        assertEval("\"Abc\"", "1", simpleSession)
        assertEval("\"aBc\"", "2", simpleSession)
        assertEval("\"abC\"", "3", simpleSession)
    }

    @Test
    fun quotedIdResolvesWithSensitiveCaseResolvesToMissing() {
        assertEvalIsMissing("\"abc\"", simpleSession, undefinedVariableMissingCompileOptions)
        assertEvalIsMissing("\"ABC\"", simpleSession, undefinedVariableMissingCompileOptions)

        //Ensure case sensitive lookup still works.
        assertEval("\"Abc\"", "1", simpleSession, undefinedVariableMissingCompileOptions)
    }

    @Test
    fun quotedIdsCantFindMismatchedCase() {
        checkInputThrowingEvaluationException(
            "\"abc\"",
            simpleSession,
            ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
            sourceLocationProperties(1L, 1L) + mapOf(Property.BINDING_NAME to "abc"),
            expectedPermissiveModeResult = "MISSING")

        checkInputThrowingEvaluationException(
            "\"ABC\"",
            simpleSession,
            ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
            sourceLocationProperties(1L, 1L) + mapOf(Property.BINDING_NAME to "ABC"),
            expectedPermissiveModeResult = "MISSING")
    }

    @Test
    fun unquotedIdIsAmbigous() {
        checkInputThrowingEvaluationException(
            "abc",
            simpleSession,
            ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
            sourceLocationProperties(1L, 1L) + mapOf(
                Property.BINDING_NAME to "abc",
                Property.BINDING_NAME_MATCHES to "Abc, aBc, abC"),
            expectedPermissiveModeResult = "MISSING")
    }

    @Test
    fun selectFromTablesQuotedIdsAreCaseSensitive() {
        assertEval("SELECT * FROM \"Abc\"", "[{n:1}]", sessionWithCaseVaryingTables)
        assertEval("SELECT * FROM \"aBc\"", "[{n:2}]", sessionWithCaseVaryingTables)
        assertEval("SELECT * FROM \"abC\"", "[{n:3}]", sessionWithCaseVaryingTables)
    }

    @Test
    fun quotedTableAliasesReferencesAreCaseSensitive() =
        assertEval(
            "SELECT \"Abc\".n AS a, \"aBc\".n AS b, \"abC\".n AS c FROM a as Abc, b as aBc, c as abC",
            "[{a:1, b:2, c:3}]",
            simpleSessionWithTables)

    @Test
    fun quotedTableAliasesAreCaseSensitive() =
        assertEval(
            "SELECT \"Abc\".n AS a, \"aBc\".n AS b, \"abC\".n AS c FROM a as \"Abc\", b as \"aBc\", c as \"abC\"",
            "[{a:1, b:2, c:3}]",
            simpleSessionWithTables)

    val tableWithCaseVaryingFields = "[{ Abc: 1, aBc: 2, abC: 3}]"

    @Test
    fun quotedStructFieldsAreCaseSensitive() =
        assertEval("SELECT s.\"Abc\" , s.\"aBc\", s.\"abC\" FROM `$tableWithCaseVaryingFields` AS s",
                   tableWithCaseVaryingFields)

    @Test
    fun unquotedStructFieldsAreAmbiguous() {
        checkInputThrowingEvaluationException(
            "SELECT s.abc FROM `$tableWithCaseVaryingFields` AS s",
            simpleSession,
            ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
            sourceLocationProperties(1L, 10L) + mapOf(
                Property.BINDING_NAME to "abc",
                Property.BINDING_NAME_MATCHES to "Abc, aBc, abC"),
            expectedPermissiveModeResult = "<<{}>>"
        )
    }

    ////////////////////////////////////////////
    private val nestedStructsLowercase = mapOf("a" to "{b:{c:{d:{e:5,f:6}}}}")
    private val globalHello = mapOf("s" to "\"hello\"")

    /**
     * Sample ion containing a collection of stores
     */
    private val stores = mapOf(
        "stores" to """
        [
          {
            id: "5",
            Id: "5",
            books: [
              {title:"A", price: 5.0, categories:["sci-fi", "action"], Title:"asdf", Price: 38.0, Categories:["history", "drama"]},
              {title:"B", price: 2.0, categories:["sci-fi", "comedy"], Title:"asdf", Price: 38.0, Categories:["history", "drama"]},
              {title:"C", price: 7.0, categories:["action", "suspense"], Title:"asdf", Price: 38.0, Categories:["history", "drama"]},
              {title:"D", price: 9.0, categories:["suspense"], Title:"asdf", Price: 38.0, Categories:["history", "drama"]},
            ]
          },
          {
            id: "6",
            Id: "64",
            books: [
              {title:"A", price: 5.0, categories:["sci-fi", "action"], Title:"asdf", Price: 38.0, Categories:["history", "drama"]},
              {title:"E", price: 9.5, categories:["fantasy", "comedy"], Title:"asdf", Price: 38.0, Categories:["history", "drama"]},

              {title:"F", price: 10.0, categories:["history"], Title:"asdf", Price: 38.0, Categories:["history", "drama"]},
            ]
          },
          {
            id: "7",
            Id: "72",
            books: [],
            Books: []
          }
        ]
        """,
        "Stores" to "1"
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
           Kumo: { },
           mochi: {
             type: "DOG",
             likes: {
               kumo: { type: "dog" },
               brownie: { type: "cat" },
             }
           },
           Mochi: { }
        }
        """,
        "Friends" to "1"
    )

    // NOTE: the versions of these tests which use unquoted identifiers exist within [EvaluatingCompilerTests]

    @Test
    fun pathDotOnly_quotedId() =
        assertEval(""" "a"."b"."c"."d"."e" """, "5", nestedStructsLowercase.toSession())

    @Test
    fun pathDotOnly_mixedIds() =
        assertEval(""" "a".b."c".d."e" """, "5", nestedStructsLowercase.toSession())
    @Test
    fun pathDotOnly_mixedIds_Inverted() =
        assertEval(""" a."b".c."d".e """, "5", nestedStructsLowercase.toSession())

    @Test
    fun pathDotMissingAttribute_quotedId() =
        assertEval(""" "a"."z" IS MISSING """, "true", nestedStructsLowercase.toSession())

    @Test
    fun pathDotMissingAttribute_mixedIds() =
        assertEval(""" a."z" IS MISSING """, "true", nestedStructsLowercase.toSession())

    @Test
    fun pathDotMissingAttribute_Inverted() =
        assertEval(""" "a".z IS MISSING """, "true", nestedStructsLowercase.toSession())


    @Test
    fun pathIndexing_quotedId() = assertEval(""" "stores"[0]."books"[2]."title" """, "\"C\"", stores.toSession())

    @Test
    fun pathFieldStructLiteral_quotedId() = assertEval("""{'a': 1, 'b': 2, 'b': 3}."a" """, "1")

    @Test
    fun pathWildcard_quotedId() = assertEval(""" "stores"[0]."books"[*]."title" """, """["A", "B", "C", "D"]""", stores.toSession())

    @Test
    fun pathUnpivotWildcard_quotedId() = assertEval(""" "friends"."kumo"."likes".*."type" """, """["dog", "human"]""", friends.toSession())

    @Test
    fun pathDoubleWildCard_quotedId() = assertEval(
        """ "stores"[*]."books"[*]."title" """,
        """["A", "B", "C", "D", "A", "E", "F"]""",
        stores.toSession()
    )

    @Test
    fun pathDoubleUnpivotWildCard_quotedId() = assertEval(
        """ "friends".*."likes".*."type" """,
        """["dog", "human", "dog", "cat"]""",
        friends.toSession()
    )


    @Test
    fun pathWildCardOverScalar_quotedId() = assertEval(
        """ "s"[*] """,
        """["hello"]""",
        globalHello.toSession()
    )

    @Test
    fun pathUnpivotWildCardOverScalar_quotedId() = assertEval(
        """ "s".*  """,
        """["hello"]""",
        globalHello.toSession()
    )
}