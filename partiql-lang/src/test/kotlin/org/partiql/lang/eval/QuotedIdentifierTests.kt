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

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.errors.PropertyValueMap
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.util.propertyValueMapOf

class QuotedIdentifierTests : EvaluatorTestBase() {

    companion object {
        private val simpleSessionEnv = mapOf(
            "Abc" to "1",
            "aBc" to "2",
            "abC" to "3"
        )

        private val sessionWithCaseVaryingTablesEnv = mapOf(
            "Def" to "[{n:1}]",
            "dEf" to "[{n:2}]",
            "deF" to "[{n:3}]"
        )

        private val simpleSessionWithTablesEnv = mapOf(
            "a" to "[{n:1}]",
            "b" to "[{n:2}]",
            "c" to "[{n:3}]",
            "" to "empty_variable_name_value"
        )

        private val nestedStructsLowercase = mapOf("n" to "{b:{c:{d:{e:5,f:6}}}}")

        private val globalHello = mapOf("s" to "\"hello\"")

        private val env = simpleSessionEnv + sessionWithCaseVaryingTablesEnv + simpleSessionWithTablesEnv + nestedStructsLowercase + globalHello

        private val FILE_PATH = "quoted-identifier.ion"

        @BeforeAll
        @JvmStatic
        fun setup() {
            EvaluationTestCase.print(FILE_PATH, emptyList(), env)
        }
    }

    private val simpleSession = simpleSessionEnv.toSession()
    private val sessionWithCaseVaryingTables = sessionWithCaseVaryingTablesEnv.toSession()
    private val simpleSessionWithTables = simpleSessionWithTablesEnv.toSession()

    private fun runEvaluatorTestCase(
        query: String,
        session: EvaluationSession = EvaluationSession.standard(),
        expectedResult: String,
        target: EvaluatorTestTarget = EvaluatorTestTarget.ALL_PIPELINES // This is ignored
    ) {
        val assertion = EvaluationTestCase.Assertion.Success(EvaluationTestCase.ALL_MODES, expectedResult)
        val case = EvaluationTestCase(query, query, assertion)
        case.append(FILE_PATH)
    }

    private fun runEvaluatorErrorTestCase(
        query: String,
        errorCode: ErrorCode,
        properties: PropertyValueMap? = null, // This is ignored
        expectedPermissiveModeResult: String? = null,
        session: EvaluationSession = EvaluationSession.standard(),
        target: EvaluatorTestTarget = EvaluatorTestTarget.ALL_PIPELINES // This is intentionally ignored
    ) {
        val assertion = when (expectedPermissiveModeResult) {
            null -> EvaluationTestCase.Assertion.Failure(EvaluationTestCase.ALL_MODES)
            else -> EvaluationTestCase.Assertion.Multi(
                EvaluationTestCase.Assertion.Failure(EvaluationTestCase.ERROR),
                EvaluationTestCase.Assertion.SuccessPartiQL(EvaluationTestCase.COERCE, expectedPermissiveModeResult, env)
            )
        }
        val case = EvaluationTestCase(query, query, EvaluationTestCase.Assertion.Failure(EvaluationTestCase.ALL_MODES))
        case.append(FILE_PATH)
    }

    private val undefinedVariableMissingCompileOptionBlock: CompileOptions.Builder.() -> Unit = {
        undefinedVariable(UndefinedVariableBehavior.MISSING)
    }

    @Test
    fun quotedIdResolvesWithSensitiveCase() {

        runEvaluatorTestCase("\"Abc\"", simpleSession, "1")
        runEvaluatorTestCase("\"aBc\"", simpleSession, "2")
        runEvaluatorTestCase("\"abC\"", simpleSession, "3")
    }

    @Test
    fun quotedIdResolvesWithSensitiveCaseResolvesToMissing() {
        runEvaluatorTestCase(
            query = "\"abc\"",
            session = simpleSession,
            expectedResult = "MISSING",
            expectedResultFormat = ExpectedResultFormat.STRICT,
            // planner & physical plan have no support for UndefinedVariableBehavior.MISSING (and may never)
            target = EvaluatorTestTarget.COMPILER_PIPELINE,
            compileOptionsBuilderBlock = { undefinedVariableMissingCompileOptionBlock() },
        )
        runEvaluatorTestCase(
            "\"ABC\"",
            session = simpleSession,
            expectedResult = "MISSING",
            expectedResultFormat = ExpectedResultFormat.STRICT,
            // planner & physical plan have no support for UndefinedVariableBehavior.MISSING (and may never)
            target = EvaluatorTestTarget.COMPILER_PIPELINE,
            compileOptionsBuilderBlock = { undefinedVariableMissingCompileOptionBlock() },
        )

        // Ensure case sensitive lookup still works.
        runEvaluatorTestCase(
            query = "\"Abc\"",
            session = simpleSession,
            expectedResult = "1",
            // planner & physical plan have no support for UndefinedVariableBehavior.MISSING (and may never)
            target = EvaluatorTestTarget.COMPILER_PIPELINE,
            compileOptionsBuilderBlock = undefinedVariableMissingCompileOptionBlock
        )
    }

    @Test
    fun quotedIdsCantFindMismatchedCase() {
        runEvaluatorErrorTestCase(
            "\"abc\"",
            ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
            propertyValueMapOf(1, 1, Property.BINDING_NAME to "abc"),
            expectedPermissiveModeResult = "MISSING",
            session = simpleSession
        )

        runEvaluatorErrorTestCase(
            "\"ABC\"",
            ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
            propertyValueMapOf(1, 1, Property.BINDING_NAME to "ABC"),
            expectedPermissiveModeResult = "MISSING",
            session = simpleSession
        )
    }

    @Test
    fun unquotedIdIsAmbigous() {
        runEvaluatorErrorTestCase(
            "abc",
            ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
            propertyValueMapOf(
                1, 1,
                Property.BINDING_NAME to "abc",
                Property.BINDING_NAME_MATCHES to "Abc, aBc, abC"
            ),
            expectedPermissiveModeResult = "MISSING",
            target = EvaluatorTestTarget.COMPILER_PIPELINE,
            session = simpleSession // Planner will never throw ambiguous binding error
        )
    }

    @Test
    fun selectFromTablesQuotedIdsAreCaseSensitive() {
        runEvaluatorTestCase("SELECT * FROM \"Def\"", sessionWithCaseVaryingTables, "$BAG_ANNOTATION::[{n:1}]")
        runEvaluatorTestCase("SELECT * FROM \"dEf\"", sessionWithCaseVaryingTables, "$BAG_ANNOTATION::[{n:2}]")
        runEvaluatorTestCase("SELECT * FROM \"deF\"", sessionWithCaseVaryingTables, "$BAG_ANNOTATION::[{n:3}]")
    }

    @Test
    fun quotedTableAliasesReferencesAreCaseSensitive() =
        runEvaluatorTestCase(
            "SELECT \"Abc\".n AS a, \"aBc\".n AS b, \"abC\".n AS c FROM a as Abc, b as aBc, c as abC",
            simpleSessionWithTables,
            "$BAG_ANNOTATION::[{a:1, b:2, c:3}]"
        )

    @Test
    fun quotedTableAliasesAreCaseSensitive() =
        runEvaluatorTestCase(
            "SELECT \"Abc\".n AS a, \"aBc\".n AS b, \"abC\".n AS c FROM a as \"Abc\", b as \"aBc\", c as \"abC\"",
            simpleSessionWithTables,
            "$BAG_ANNOTATION::[{a:1, b:2, c:3}]"
        )

    val tableWithCaseVaryingFields = "$BAG_ANNOTATION::[{ Abc: 1, aBc: 2, abC: 3}]"

    @Test
    fun quotedStructFieldsAreCaseSensitive() =
        runEvaluatorTestCase(
            "SELECT s.\"Abc\" , s.\"aBc\", s.\"abC\" FROM `$tableWithCaseVaryingFields` AS s",
            expectedResult = tableWithCaseVaryingFields
        )

    @Test
    fun unquotedStructFieldsAreAmbiguous() {
        runEvaluatorErrorTestCase(
            "SELECT s.abc FROM `$tableWithCaseVaryingFields` AS s",
            ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
            propertyValueMapOf(
                1, 10,
                Property.BINDING_NAME to "abc",
                Property.BINDING_NAME_MATCHES to "Abc, aBc, abC"
            ),
            expectedPermissiveModeResult = "<<{}>>",
            session = simpleSession
        )
    }

    // //////////////////////////////////////////

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
        runEvaluatorTestCase(""" "n"."b"."c"."d"."e" """, nestedStructsLowercase.toSession(), "5")

    @Test
    fun pathDotOnly_mixedIds() =
        runEvaluatorTestCase(""" "n".b."c".d."e" """, nestedStructsLowercase.toSession(), "5")
    @Test
    fun pathDotOnly_mixedIds_Inverted() =
        runEvaluatorTestCase(""" n."b".c."d".e """, nestedStructsLowercase.toSession(), "5")

    @Test
    fun pathDotMissingAttribute_quotedId() =
        runEvaluatorTestCase(""" "n"."z" IS MISSING """, nestedStructsLowercase.toSession(), "true")

    @Test
    fun pathDotMissingAttribute_mixedIds() =
        runEvaluatorTestCase(""" n."z" IS MISSING """, nestedStructsLowercase.toSession(), "true")

    @Test
    fun pathDotMissingAttribute_Inverted() =
        runEvaluatorTestCase(""" "n".z IS MISSING """, nestedStructsLowercase.toSession(), "true")

    @Test
    fun pathIndexing_quotedId() =
        runEvaluatorTestCase(""" "stores"[0]."books"[2]."title" """, stores.toSession(), "\"C\"")

    @Test
    fun pathFieldStructLiteral_quotedId() =
        runEvaluatorTestCase("""{'a': 1, 'b': 2, 'b': 3}."a" """, expectedResult = "1")

    @Test
    fun pathWildcard_quotedId() = runEvaluatorTestCase(
        """ "stores"[0]."books"[*]."title" """,
        stores.toSession(),
        """$BAG_ANNOTATION::["A", "B", "C", "D"]"""
    )

    @Test
    fun pathUnpivotWildcard_quotedId() = runEvaluatorTestCase(
        """ "friends"."kumo"."likes".*."type" """,
        friends.toSession(),
        """$BAG_ANNOTATION::["dog", "human"]"""
    )

    @Test
    fun pathDoubleWildCard_quotedId() = runEvaluatorTestCase(
        """ "stores"[*]."books"[*]."title" """,
        stores.toSession(),
        """$BAG_ANNOTATION::["A", "B", "C", "D", "A", "E", "F"]"""
    )

    @Test
    fun pathDoubleUnpivotWildCard_quotedId() = runEvaluatorTestCase(
        """ "friends".*."likes".*."type" """,
        friends.toSession(),
        """$BAG_ANNOTATION::["dog", "human", "dog", "cat"]"""
    )

    @Test
    fun pathWildCardOverScalar_quotedId() = runEvaluatorTestCase(
        """ "s"[*] """,
        globalHello.toSession(),
        """$BAG_ANNOTATION::["hello"]"""
    )

    @Test
    fun pathUnpivotWildCardOverScalar_quotedId() = runEvaluatorTestCase(
        """ "s".*  """,
        globalHello.toSession(),
        """$BAG_ANNOTATION::["hello"]"""
    )
}
