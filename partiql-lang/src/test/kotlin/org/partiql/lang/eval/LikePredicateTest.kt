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
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.lang.util.propertyValueMapOf

class LikePredicateTest : EvaluatorTestBase() {

    private val animals = mapOf(
        "animals" to """
        [
          {name: "Kumo", type: "dog"},
          {name: "Mochi", type: "dog"},
          {name: "Lilikoi", type: "unicorn"},
        ]
        """
    ).toSession()

    private val animalsWithNulls = mapOf(
        "animalsWithNulls" to """
        [
          {name: null, type: "dog"},
          {name: null, type: "dog"},
          {name: null, type: "unicorn"},
        ]
        """
    ).toSession()

    @Test
    fun emptyTextUnderscorePattern() =
        runEvaluatorTestCase("""SELECT * FROM `[true]` as a WHERE '' LIKE '_'  """, animals, "$BAG_ANNOTATION::[]")

    @Test
    fun emptyTextPercentPattern() = runEvaluatorTestCase(
        """SELECT * FROM `[true]` as a WHERE '' LIKE '%'  """, animals,
        "$BAG_ANNOTATION::[{_1: true}]"
    )

    @Test
    fun allLiteralsAndEscapeIsNull() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'A' LIKE 'B' ESCAPE null """,
        animals,
        "$BAG_ANNOTATION::[]"
    )

    @Test
    fun valueLiteralPatternNull() =
        runEvaluatorTestCase("""SELECT * FROM animals as a WHERE 'A' LIKE null """, animals, "$BAG_ANNOTATION::[]")

    @Test
    fun valueNullPatternLiteral() =
        runEvaluatorTestCase("""SELECT * FROM animals as a WHERE null LIKE 'A' """, animals, "$BAG_ANNOTATION::[]")

    @Test
    fun valueNullPatternLiteralEscapeNull() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE null LIKE 'A' ESCAPE null""",
        animals,
        "$BAG_ANNOTATION::[]"
    )

    @Test
    fun valueNullPatternNullEscapeLiteral() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE null LIKE null ESCAPE '['""",
        animals,
        "$BAG_ANNOTATION::[]"
    )

    @Test
    fun valueLiteralPatternNullEscapeNull() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'A' LIKE null ESCAPE null""",
        animals,
        "$BAG_ANNOTATION::[]"
    )

    @Test
    fun valueNullPatternNullEscapeNull() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE null LIKE null ESCAPE null""",
        animals,
        "$BAG_ANNOTATION::[]"
    )

    @Test
    fun typeIsChecked() {
        // Specify the types we'll test
        data class ParamType(val precedence: Int)
        val NULL = ParamType(1)
        val INT = ParamType(2) // will throw error
        val STR = ParamType(3)

        // references are deferred to runtime and take a separate compile path than literals
        data class Param(val param: String, val type: ParamType, val escParam: String = param)
        val types = listOf(
            Param("null", NULL),
            Param("a._null_", NULL),
            Param("123", INT),
            Param("a.num", INT),
            Param("'string'", STR, "'\\'"),
            Param("a.str", STR, "a.esc")
        )

        // Run the test with the given parameters
        fun runTest(whereClause: String, vararg types: Param) {
            val input = """$BAG_ANNOTATION::[{num: 1, str: "string", esc: "\\"}]"""
            val session = mapOf("Object" to input).toSession()
            val query = "Select * From Object a Where " + whereClause

            when (types.map { it.type }.minByOrNull { it.precedence }) {
                NULL -> runEvaluatorTestCase(query, session, "$BAG_ANNOTATION::[]")
                STR -> runEvaluatorTestCase(query, session, input)
                INT -> {
                    runEvaluatorErrorTestCase(
                        query = query,
                        expectedErrorCode = ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
                        session = session,
                    )
                }
            }
        }

        // Try each combination of types as input to the test
        for (value in types) for (pattern in types) {
            runTest("${value.param} LIKE ${pattern.param}", value, pattern)
            for (escape in types) {
                runTest("${value.param} LIKE ${pattern.param} ESCAPE ${escape.escParam}", value, pattern, escape)
            }
        }
    }

    @Test
    fun textAndPatternEmpty() = runEvaluatorTestCase(
        """ SELECT * FROM animals WHERE '' LIKE '' """,
        animals,
        """
             $BAG_ANNOTATION::[
                {name: "Kumo", type: "dog"},
                {name:"Mochi",type:"dog"},
                {name:"Lilikoi",type:"unicorn"}
              ]
            """
    )

    @Test
    fun textNonEmptyPatternEmpty() = runEvaluatorTestCase(
        """ SELECT * FROM animals WHERE 'Kumo' LIKE '' """,
        animals,
        """
             $BAG_ANNOTATION::[]
            """
    )

    @Test
    fun noEscapeAllArgsLiteralsMatches() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'Kumo' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsMismatchCase() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'KuMo' """,
        animals,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsMismatchPattern() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'xxx' LIKE 'Kumo' """,
        animals,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsMatchUnderscore() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K_mo' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsNoMatchUnderscore() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kuumo' LIKE 'K_mo' """,
        animals,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsNoMatchUnderscoreExtraChar() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'KKumo' LIKE 'K_mo' """,
        animals,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsMatchConsecutiveUnderscores() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K__o' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsMatch2UnderscoresNonConsecutive() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE '_u_o' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsMatchUnderscoresAtEnd() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'Kum_' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsMatchPercentage() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'Ku%o' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsNoMatchPercentageExtraCharBefore() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'KKumo' LIKE 'Ku%o' """,
        animals,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsNoMatchPercentageExtraCharAfter() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumol' LIKE 'Ku%o' """,
        animals,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsMatch2PercentagesConsecutive() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K%%o' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsMatch2PercentagesNonConsecutive() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K%m%' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsMatchPercentageAsFirst() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE '%umo' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsMatchPercentageAsLast() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'Kum%' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsPercentageAndUnderscore() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K_%mo' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsPercentageAndUnderscoreNonConsecutive() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K_m%' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsAllUnderscores() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE '____' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsJustPercentage() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE '%' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeAllArgsLiteralsEmptyStringAndJustPercentage() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '' LIKE '%' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun EscapePercentageAllArgsLiterals() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '%' LIKE '[%' ESCAPE '[' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaPercentage() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '100%' LIKE '1%[%' ESCAPE '[' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name:"Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun EscapePercentageWithBackSlashAllArgsLiteralsPatternWithMetaPercentage() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '100%' LIKE '1%\%' ESCAPE '\' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaUnderscore() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '100%' LIKE '1__[%' ESCAPE '[' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaPercentAtStart() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '%100' LIKE '[%%' ESCAPE '[' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaPercentAtStartFollowedByUnderscore() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '%100' LIKE '[%_00' ESCAPE '[' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaPercentAtStartFollowedByUnderscoreNoMatch() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '%1XX' LIKE '[%_00' ESCAPE '[' """,
        animals,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun MultipleEscapesNoMeta() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '1_000_000%' LIKE '1[_000[_000[%' ESCAPE '[' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun MultipleEscapesWithMeta() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '1_000_000%' LIKE '1[____[_%[%' ESCAPE '[' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun MultipleEscapesWithMetaAtStart() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '1_000_000%' LIKE '_[_%[_%[%' ESCAPE '[' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeValueIsBinding() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE a.name LIKE 'Kumo' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"}
          ]
        """
    )

    @Test
    fun noEscapeValueIsStringAppendExpression() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE a.name || 'xx' LIKE '%xx' """,
        animals,
        """
          $BAG_ANNOTATION::[
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """
    )

    @Test
    fun noEscapeValueAndPatternAreBindings() = runEvaluatorTestCase(
        """SELECT a.name FROM
                  `[
                   { name:"Abcd", pattern:"A___" },
                   { name:"100",  pattern:"1%0" }
                  ]` as a
               WHERE a.name LIKE a.pattern """,
        expectedResult = """
                  $BAG_ANNOTATION::[
                     { name:"Abcd" },
                     { name:"100"}
                  ]
                """
    )

    @Test
    fun EscapeLiteralValueAndPatternAreBindings() = runEvaluatorTestCase(
        """SELECT a.name FROM
                  `[
                   { name:"Abcd", pattern:"A___" },
                   { name:"100%",  pattern:"1%0\\%" }
                  ]` as a
               WHERE a.name LIKE a.pattern ESCAPE '\' """,
        expectedResult = """
                  $BAG_ANNOTATION::[
                     { name:"Abcd" },
                     { name:"100%"}
                  ]
                """
    )

    @Test
    fun EscapeValueAndPatternAreBindings() = runEvaluatorTestCase(
        """SELECT a.name FROM
                  `[
                   { name:"Abcd", pattern:"A___" , escapeChar:'['},
                   { name:"100%",  pattern:"1%0[%", escapeChar: '['}
                  ]` as a
               WHERE a.name LIKE a.pattern ESCAPE a.escapeChar """,
        expectedResult = """
                  $BAG_ANNOTATION::[
                     { name:"Abcd" },
                     { name:"100%"}
                  ]
                """
    )

    @Test
    fun NotLikeEscapeValueAndPatternAreBindings() = runEvaluatorTestCase(
        """SELECT a.name FROM
                  `[
                   { name:"Abcd", pattern:"A__" , escapeChar:'['},
                   { name:"1000%",  pattern:"1_0[%", escapeChar: '['}
                  ]` as a
               WHERE a.name NOT LIKE a.pattern ESCAPE a.escapeChar """,
        expectedResult = """
                  $BAG_ANNOTATION::[
                     { name:"Abcd" },
                     { name:"1000%"}
                  ]
                """
    )

    @Test
    fun emptyStringAsEscape() = runEvaluatorErrorTestCase(
        "SELECT * FROM <<>> AS a WHERE '%' LIKE '%' ESCAPE ''",
        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
        expectedErrorContext = propertyValueMapOf(1, 51)
    )

    @Test
    fun moreThanOneCharacterEscape() = runEvaluatorErrorTestCase(
        "SELECT * FROM <<>> AS a WHERE '%' LIKE '%' ESCAPE '[]'",
        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
        expectedErrorContext = propertyValueMapOf(1, 51)
    )

    @Test
    fun escapeByItself() = runEvaluatorErrorTestCase(
        "SELECT * FROM <<>> AS a WHERE 'aaaaa' LIKE '[' ESCAPE '['",
        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
        expectedErrorContext = propertyValueMapOf(1, 44, Property.LIKE_PATTERN to "[", Property.LIKE_ESCAPE to "[")
    )

    @Test
    fun escapeWithoutWildcard() = runEvaluatorErrorTestCase(
        "SELECT * FROM <<>> AS a WHERE 'aaaaa' LIKE '[a' ESCAPE '['",
        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
        expectedErrorContext = propertyValueMapOf(1, 44, Property.LIKE_PATTERN to "[a", Property.LIKE_ESCAPE to "[")
    )

    @Test
    fun valueNotAString() = runEvaluatorErrorTestCase(
        "SELECT * FROM <<>> AS a WHERE 1 LIKE 'a' ESCAPE '['",
        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
        expectedErrorContext = propertyValueMapOf(1, 33, Property.LIKE_VALUE to "1")
    )

    @Test
    fun patternNotAString() = runEvaluatorErrorTestCase(
        "SELECT * FROM <<>> AS a WHERE 'a' LIKE 1 ESCAPE '['",
        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
        expectedErrorContext = propertyValueMapOf(1, 35, Property.LIKE_PATTERN to "1", Property.LIKE_ESCAPE to "'['")
    )

    @Test
    fun escapeNotAString() = runEvaluatorErrorTestCase(
        // column is marked at the position of LIKE
        "SELECT * FROM <<>> AS a WHERE 'a' LIKE 'a' ESCAPE 1",
        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
        expectedErrorContext = propertyValueMapOf(1, 35, Property.LIKE_PATTERN to "'a'", Property.LIKE_ESCAPE to "1")
    )

    @Test
    fun valueIsNull() =
        runEvaluatorTestCase("SELECT * FROM <<>> AS a WHERE null LIKE 'a' ESCAPE '['", expectedResult = "$BAG_ANNOTATION::[]")

    @Test
    fun patternIsNull() =
        runEvaluatorTestCase("SELECT * FROM <<>> AS a WHERE 'a' LIKE null ESCAPE '['", expectedResult = "$BAG_ANNOTATION::[]")

    @Test
    fun escapeIsNull() =
        runEvaluatorTestCase("SELECT * FROM <<>> AS a WHERE 'a' LIKE 'a' ESCAPE null", expectedResult = "$BAG_ANNOTATION::[]")

    @Test
    fun nonLiteralsMissingValue() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE a.xxx LIKE '%' """,
        animals,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun nonLiteralsMissingPattern() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE a.name LIKE a.xxx """,
        animals,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun nonLiteralsMissingEscape() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE a.name LIKE '%' ESCAPE a.xxx""",
        animals,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun nonLiteralsNullValue() = runEvaluatorTestCase(
        """SELECT * FROM animalsWithNulls as a WHERE a.name LIKE '%' """,
        animalsWithNulls,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun nonLiteralsNullPattern() = runEvaluatorTestCase(
        """SELECT * FROM animalsWithNulls as a WHERE a.type LIKE a.name """,
        animalsWithNulls,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun nonLiteralsNullEscape() = runEvaluatorTestCase(
        """SELECT * FROM animalsWithNulls as a WHERE a.type LIKE '%' ESCAPE a.name""",
        animalsWithNulls,
        """
          $BAG_ANNOTATION::[]
        """
    )

    @Test
    fun nonLiteralsNonStringEscape() = runEvaluatorErrorTestCase(
        "SELECT * FROM `[{name:1, type:\"a\"}]` as a WHERE a.type LIKE '%' ESCAPE a.name",
        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
        expectedErrorContext = propertyValueMapOf(1, 56, Property.LIKE_PATTERN to "'%'", Property.LIKE_ESCAPE to "1")
    )

    @Test
    fun nonLiteralsNonStringPattern() = runEvaluatorErrorTestCase(
        "SELECT * FROM `[{name:1, type:\"a\"}]` as a WHERE a.type LIKE a.name",
        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
        expectedErrorContext = propertyValueMapOf(1, 56, Property.LIKE_PATTERN to "1")
    )

    @Test
    fun nonLiteralsNonStringValue() = runEvaluatorErrorTestCase(
        "SELECT * FROM `[{name:1, type:\"a\"}]` as a WHERE a.name LIKE a.type ",
        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
        expectedErrorContext = propertyValueMapOf(1, 56, Property.LIKE_VALUE to "1")
    )

    /** Regression test for: https://github.com/partiql/partiql-lang-kotlin/issues/32 */
    @Test
    fun multiCodepointPattern() = runEvaluatorTestCase("'😍' LIKE '😍'", expectedResult = "true")
}
