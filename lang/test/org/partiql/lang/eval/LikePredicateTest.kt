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

import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.partiql.lang.SqlException
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.softAssert
import kotlin.test.assertFailsWith

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
        runEvaluatorTestCase("""SELECT * FROM `[true]` as a WHERE '' LIKE '_'  """, "[]", animals)

    @Test
    fun emptyTextPercentPattern() = runEvaluatorTestCase(
        """SELECT * FROM `[true]` as a WHERE '' LIKE '%'  """, "[{_1: true}]",
        animals
    )

    @Test
    fun allLiteralsAndEscapeIsNull() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'A' LIKE 'B' ESCAPE null """,
        "[]",
        animals
    )

    @Test
    fun valueLiteralPatternNull() =
        runEvaluatorTestCase("""SELECT * FROM animals as a WHERE 'A' LIKE null """, "[]", animals)

    @Test
    fun valueNullPatternLiteral() =
        runEvaluatorTestCase("""SELECT * FROM animals as a WHERE null LIKE 'A' """, "[]", animals)

    @Test
    fun valueNullPatternLiteralEscapeNull() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE null LIKE 'A' ESCAPE null""",
        "[]",
        animals
    )

    @Test
    fun valueNullPatternNullEscapeLiteral() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE null LIKE null ESCAPE '['""",
        "[]",
        animals
    )

    @Test
    fun valueLiteralPatternNullEscapeNull() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'A' LIKE null ESCAPE null""",
        "[]",
        animals
    )

    @Test
    fun valueNullPatternNullEscapeNull() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE null LIKE null ESCAPE null""",
        "[]",
        animals
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
        fun runTest(whereClause: String, softly: SoftAssertions, vararg types: Param) {
            val input = """[{num: 1, str: "string", esc: "\\"}]"""
            val session = mapOf("Object" to input).toSession()
            val query = "Select * From Object a Where " + whereClause

            softly.assertThatCode {
                when (types.map { it.type }.minByOrNull { it.precedence }) {
                    NULL -> runEvaluatorTestCase(query, "[]", session)
                    INT -> {
                        val ex = assertFailsWith<SqlException>(message = query) {
                            eval(query, session = session).toList()
                        }
                        assertEquals(query, ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS, ex.errorCode)
                    }
                    STR -> runEvaluatorTestCase(query, input, session)
                }
            }.`as`(query).doesNotThrowAnyException()
        }

        softAssert {
            // Try each combination of types as input to the test
            for (value in types) for (pattern in types) {
                runTest("${value.param} LIKE ${pattern.param}", this, value, pattern)
                for (escape in types) {
                    runTest("${value.param} LIKE ${pattern.param} ESCAPE ${escape.escParam}", this, value, pattern, escape)
                }
            }
        }
    }

    @Test
    fun textAndPatternEmpty() = runEvaluatorTestCase(
        """ SELECT * FROM animals WHERE '' LIKE '' """,
        """
             [
                {name: "Kumo", type: "dog"},
                {name:"Mochi",type:"dog"},
                {name:"Lilikoi",type:"unicorn"}
              ]
            """,
        animals
    )

    @Test
    fun textNonEmptyPatternEmpty() = runEvaluatorTestCase(
        """ SELECT * FROM animals WHERE 'Kumo' LIKE '' """,
        """
             []
            """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsMatches() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'Kumo' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsMismatchCase() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'KuMo' """,
        """
          []
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsMismatchPattern() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'xxx' LIKE 'Kumo' """,
        """
          []
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsMatchUnderscore() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K_mo' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsNoMatchUnderscore() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kuumo' LIKE 'K_mo' """,
        """
          []
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsNoMatchUnderscoreExtraChar() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'KKumo' LIKE 'K_mo' """,
        """
          []
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsMatchConsecutiveUnderscores() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K__o' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsMatch2UnderscoresNonConsecutive() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE '_u_o' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsMatchUnderscoresAtEnd() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'Kum_' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsMatchPercentage() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'Ku%o' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsNoMatchPercentageExtraCharBefore() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'KKumo' LIKE 'Ku%o' """,
        """
          []
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsNoMatchPercentageExtraCharAfter() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumol' LIKE 'Ku%o' """,
        """
          []
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsMatch2PercentagesConsecutive() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K%%o' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsMatch2PercentagesNonConsecutive() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K%m%' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsMatchPercentageAsFirst() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE '%umo' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsMatchPercentageAsLast() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'Kum%' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsPercentageAndUnderscore() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K_%mo' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsPercentageAndUnderscoreNonConsecutive() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K_m%' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsAllUnderscores() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE '____' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsJustPercentage() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE 'Kumo' LIKE '%' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeAllArgsLiteralsEmptyStringAndJustPercentage() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '' LIKE '%' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun EscapePercentageAllArgsLiterals() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '%' LIKE '[%' ESCAPE '[' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaPercentage() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '100%' LIKE '1%[%' ESCAPE '[' """,
        """
          [
            {name:"Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun EscapePercentageWithBackSlashAllArgsLiteralsPatternWithMetaPercentage() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '100%' LIKE '1%\%' ESCAPE '\' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaUnderscore() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '100%' LIKE '1__[%' ESCAPE '[' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaPercentAtStart() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '%100' LIKE '[%%' ESCAPE '[' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaPercentAtStartFollowedByUnderscore() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '%100' LIKE '[%_00' ESCAPE '[' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaPercentAtStartFollowedByUnderscoreNoMatch() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '%1XX' LIKE '[%_00' ESCAPE '[' """,
        """
          []
        """,
        animals
    )

    @Test
    fun MultipleEscapesNoMeta() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '1_000_000%' LIKE '1[_000[_000[%' ESCAPE '[' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun MultipleEscapesWithMeta() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '1_000_000%' LIKE '1[____[_%[%' ESCAPE '[' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun MultipleEscapesWithMetaAtStart() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE '1_000_000%' LIKE '_[_%[_%[%' ESCAPE '[' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeValueIsBinding() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE a.name LIKE 'Kumo' """,
        """
          [
            {name: "Kumo", type: "dog"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeValueIsStringAppendExpression() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE a.name || 'xx' LIKE '%xx' """,
        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
        animals
    )

    @Test
    fun noEscapeValueAndPatternAreBindings() = runEvaluatorTestCase(
        """SELECT a.name FROM
                  `[
                   { name:"Abcd", pattern:"A___" },
                   { name:"100",  pattern:"1%0" }
                  ]` as a
               WHERE a.name LIKE a.pattern """,
        """
          [
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
        """
          [
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
        """
          [
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
        """
          [
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
        expectedErrorContext = propertyValueMapOf(1, 35, Property.LIKE_PATTERN to "1", Property.LIKE_ESCAPE to "\"[\"")
    )

    @Test
    fun escapeNotAString() = runEvaluatorErrorTestCase(
        // column is marked at the position of LIKE
        "SELECT * FROM <<>> AS a WHERE 'a' LIKE 'a' ESCAPE 1",
        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
        expectedErrorContext = propertyValueMapOf(1, 35, Property.LIKE_PATTERN to "\"a\"", Property.LIKE_ESCAPE to "1")
    )

    @Test
    fun valueIsNull() =
        runEvaluatorTestCase("SELECT * FROM <<>> AS a WHERE null LIKE 'a' ESCAPE '['", "[]")

    @Test
    fun patternIsNull() =
        runEvaluatorTestCase("SELECT * FROM <<>> AS a WHERE 'a' LIKE null ESCAPE '['", "[]")

    @Test
    fun escapeIsNull() =
        runEvaluatorTestCase("SELECT * FROM <<>> AS a WHERE 'a' LIKE 'a' ESCAPE null", "[]")

    @Test
    fun nonLiteralsMissingValue() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE a.xxx LIKE '%' """,
        """
          []
        """,
        animals
    )

    @Test
    fun nonLiteralsMissingPattern() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE a.name LIKE a.xxx """,
        """
          []
        """,
        animals
    )

    @Test
    fun nonLiteralsMissingEscape() = runEvaluatorTestCase(
        """SELECT * FROM animals as a WHERE a.name LIKE '%' ESCAPE a.xxx""",
        """
          []
        """,
        animals
    )

    @Test
    fun nonLiteralsNullValue() = runEvaluatorTestCase(
        """SELECT * FROM animalsWithNulls as a WHERE a.name LIKE '%' """,
        """
          []
        """,
        animalsWithNulls
    )

    @Test
    fun nonLiteralsNullPattern() = runEvaluatorTestCase(
        """SELECT * FROM animalsWithNulls as a WHERE a.type LIKE a.name """,
        """
          []
        """,
        animalsWithNulls
    )

    @Test
    fun nonLiteralsNullEscape() = runEvaluatorTestCase(
        """SELECT * FROM animalsWithNulls as a WHERE a.type LIKE '%' ESCAPE a.name""",
        """
          []
        """,
        animalsWithNulls
    )

    @Test
    fun nonLiteralsNonStringEscape() = runEvaluatorErrorTestCase(
        "SELECT * FROM `[{name:1, type:\"a\"}]` as a WHERE a.type LIKE '%' ESCAPE a.name",
        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
        expectedErrorContext = propertyValueMapOf(1, 56, Property.LIKE_PATTERN to "\"%\"", Property.LIKE_ESCAPE to "1")
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
    fun multiCodepointPattern() = runEvaluatorTestCase("'😍' LIKE '😍'", "true")
}
