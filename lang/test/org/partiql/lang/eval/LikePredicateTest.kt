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

import org.partiql.lang.*
import org.partiql.lang.errors.*
import org.partiql.lang.util.*
import org.assertj.core.api.*
import org.junit.*
import org.junit.Test
import kotlin.test.*

class LikePredicateTest : EvaluatorTestBase() {

    private val animals = mapOf("animals" to """
        [
          {name: "Kumo", type: "dog"},
          {name: "Mochi", type: "dog"},
          {name: "Lilikoi", type: "unicorn"},
        ]
        """).toSession()

    private val animalsWithNulls = mapOf("animalsWithNulls" to """
        [
          {name: null, type: "dog"},
          {name: null, type: "dog"},
          {name: null, type: "unicorn"},
        ]
        """).toSession()


    @Test
    fun emptyTextUnderscorePattern() = assertEval("""SELECT * FROM `[true]` as a WHERE '' LIKE '_'  """, "[]", animals)

    @Test
    fun emptyTextPercentPattern() = assertEval("""SELECT * FROM `[true]` as a WHERE '' LIKE '%'  """, "[{_1: true}]",
                                          animals)


    @Test
    fun allLiteralsAndEscapeIsNull() = assertEval("""SELECT * FROM animals as a WHERE 'A' LIKE 'B' ESCAPE null """,
                                                  "[]",
                                                  animals)

    @Test
    fun valueLiteralPatternNull() = assertEval("""SELECT * FROM animals as a WHERE 'A' LIKE null """, "[]", animals)

    @Test
    fun valueNullPatternLiteral() = assertEval("""SELECT * FROM animals as a WHERE null LIKE 'A' """, "[]", animals)

    @Test
    fun valueNullPatternLiteralEscapeNull() = assertEval("""SELECT * FROM animals as a WHERE null LIKE 'A' ESCAPE null""",
                                                         "[]",
                                                         animals)

    @Test
    fun valueNullPatternNullEscapeLiteral() = assertEval("""SELECT * FROM animals as a WHERE null LIKE null ESCAPE '['""",
                                                         "[]",
                                                         animals)

    @Test
    fun valueLiteralPatternNullEscapeNull() = assertEval("""SELECT * FROM animals as a WHERE 'A' LIKE null ESCAPE null""",
                                                         "[]",
                                                         animals)

    @Test
    fun valueNullPatternNullEscapeNull() = assertEval("""SELECT * FROM animals as a WHERE null LIKE null ESCAPE null""",
                                                      "[]",
                                                      animals)

    @Test
    fun typeIsChecked() {
        // Specify the types we'll test
        data class ParamType(val precedence : Int)
        val NULL = ParamType(1)
        val INT = ParamType(2) // will throw error
        val STR = ParamType(3)

        // references are deferred to runtime and take a separate compile path than literals
        data class Param(val param : String, val type : ParamType, val escParam : String = param)
        val types = listOf(
            Param("null", NULL),
            Param("a._null_", NULL),
            Param("123", INT),
            Param("a.num", INT),
            Param("'string'", STR, "'\\'"),
            Param("a.str", STR, "a.esc")
        )

        // Run the test with the given parameters
        fun runTest(whereClause : String, softly : SoftAssertions, vararg types : Param) {
            val input = """[{num: 1, str: "string", esc: "\\"}]"""
            val session = mapOf("Object" to input).toSession()
            val query = "Select * From Object a Where " + whereClause

            softly.assertThatCode {
                when (types.map{ it.type }.minBy{ it.precedence }) {
                    NULL -> assertEval(query, "[]", session)
                    INT -> {
                        val ex = assertFailsWith<SqlException>(message = query) {
                            eval(query, session = session).toList()
                        }
                        assertEquals(query, ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS, ex.errorCode)
                    }
                    STR -> assertEval(query, input, session)
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
        fun textAndPatternEmpty() = assertEval(""" SELECT * FROM animals WHERE '' LIKE '' """, """
             [
                {name: "Kumo", type: "dog"},
                {name:"Mochi",type:"dog"},
                {name:"Lilikoi",type:"unicorn"}
              ]
            """, animals)

    @Test
    fun textNonEmptyPatternEmpty() = assertEval(""" SELECT * FROM animals WHERE 'Kumo' LIKE '' """, """
             []
            """, animals)


    @Test
    fun noEscapeAllArgsLiteralsMatches() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE 'Kumo' """, """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """, animals)

    @Test
    fun noEscapeAllArgsLiteralsMismatchCase() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE 'KuMo' """,
                                                           """
          []
        """,
                                                           animals)


    @Test
    fun noEscapeAllArgsLiteralsMismatchPattern() = assertEval("""SELECT * FROM animals as a WHERE 'xxx' LIKE 'Kumo' """,
                                                              """
          []
        """,
                                                              animals)

    @Test
    fun noEscapeAllArgsLiteralsMatchUnderscore() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K_mo' """,
                                                              """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                              animals)

    @Test
    fun noEscapeAllArgsLiteralsNoMatchUnderscore() = assertEval("""SELECT * FROM animals as a WHERE 'Kuumo' LIKE 'K_mo' """,
                                                                """
          []
        """,
                                                                animals)

    @Test
    fun noEscapeAllArgsLiteralsNoMatchUnderscoreExtraChar() = assertEval("""SELECT * FROM animals as a WHERE 'KKumo' LIKE 'K_mo' """,
                                                                         """
          []
        """,
                                                                         animals)

    @Test
    fun noEscapeAllArgsLiteralsMatchConsecutiveUnderscores() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K__o' """,
                                                                          """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                          animals)

    @Test
    fun noEscapeAllArgsLiteralsMatch2UnderscoresNonConsecutive() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE '_u_o' """,
                                                                              """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                              animals)

    @Test
    fun noEscapeAllArgsLiteralsMatchUnderscoresAtEnd() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE 'Kum_' """,
                                                                    """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                    animals)

    @Test
    fun noEscapeAllArgsLiteralsMatchPercentage() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE 'Ku%o' """,
                                                              """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                              animals)

    @Test
    fun noEscapeAllArgsLiteralsNoMatchPercentageExtraCharBefore() = assertEval("""SELECT * FROM animals as a WHERE 'KKumo' LIKE 'Ku%o' """,
                                                                               """
          []
        """,
                                                                               animals)

    @Test
    fun noEscapeAllArgsLiteralsNoMatchPercentageExtraCharAfter() = assertEval("""SELECT * FROM animals as a WHERE 'Kumol' LIKE 'Ku%o' """,
                                                                              """
          []
        """,
                                                                              animals)


    @Test
    fun noEscapeAllArgsLiteralsMatch2PercentagesConsecutive() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K%%o' """,
                                                                           """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                           animals)

    @Test
    fun noEscapeAllArgsLiteralsMatch2PercentagesNonConsecutive() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K%m%' """,
                                                                              """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                              animals)

    @Test
    fun noEscapeAllArgsLiteralsMatchPercentageAsFirst() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE '%umo' """,
                                                                     """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                     animals)

    @Test
    fun noEscapeAllArgsLiteralsMatchPercentageAsLast() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE 'Kum%' """,
                                                                    """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                    animals)


    @Test
    fun noEscapeAllArgsLiteralsPercentageAndUnderscore() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K_%mo' """,
                                                                      """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                      animals)

    @Test
    fun noEscapeAllArgsLiteralsPercentageAndUnderscoreNonConsecutive() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE 'K_m%' """,
                                                                                    """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                                    animals)

    @Test
    fun noEscapeAllArgsLiteralsAllUnderscores() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE '____' """,
                                                             """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                             animals)

    @Test
    fun noEscapeAllArgsLiteralsJustPercentage() = assertEval("""SELECT * FROM animals as a WHERE 'Kumo' LIKE '%' """,
                                                             """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                             animals)

    @Test
    fun noEscapeAllArgsLiteralsEmptyStringAndJustPercentage() = assertEval("""SELECT * FROM animals as a WHERE '' LIKE '%' """,
                                                                           """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                           animals)

    @Test
    fun EscapePercentageAllArgsLiterals() = assertEval("""SELECT * FROM animals as a WHERE '%' LIKE '[%' ESCAPE '[' """,
                                                       """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                       animals)


    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaPercentage() = assertEval("""SELECT * FROM animals as a WHERE '100%' LIKE '1%[%' ESCAPE '[' """,
                                                                                """
          [
            {name:"Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                                animals)

    @Test
    fun EscapePercentageWithBackSlashAllArgsLiteralsPatternWithMetaPercentage() = assertEval("""SELECT * FROM animals as a WHERE '100%' LIKE '1%\%' ESCAPE '\' """,
                                                                                             """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                                             animals)

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaUnderscore() = assertEval("""SELECT * FROM animals as a WHERE '100%' LIKE '1__[%' ESCAPE '[' """,
                                                                                """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                                animals)

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaPercentAtStart() = assertEval("""SELECT * FROM animals as a WHERE '%100' LIKE '[%%' ESCAPE '[' """,
                                                                                    """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                                    animals)

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaPercentAtStartFollowedByUnderscore() = assertEval("""SELECT * FROM animals as a WHERE '%100' LIKE '[%_00' ESCAPE '[' """,
                                                                                                        """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                                                                        animals)

    @Test
    fun EscapePercentageAllArgsLiteralsPatternWithMetaPercentAtStartFollowedByUnderscoreNoMatch() = assertEval("""SELECT * FROM animals as a WHERE '%1XX' LIKE '[%_00' ESCAPE '[' """,
                                                                                                               """
          []
        """,
                                                                                                               animals)

    @Test
    fun MultipleEscapesNoMeta() = assertEval("""SELECT * FROM animals as a WHERE '1_000_000%' LIKE '1[_000[_000[%' ESCAPE '[' """,
                                             """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                             animals)

    @Test
    fun MultipleEscapesWithMeta() = assertEval("""SELECT * FROM animals as a WHERE '1_000_000%' LIKE '1[____[_%[%' ESCAPE '[' """,
                                               """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                               animals)

    @Test
    fun MultipleEscapesWithMetaAtStart() = assertEval("""SELECT * FROM animals as a WHERE '1_000_000%' LIKE '_[_%[_%[%' ESCAPE '[' """,
                                                      """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                      animals)

    @Test
    fun noEscapeValueIsBinding() = assertEval("""SELECT * FROM animals as a WHERE a.name LIKE 'Kumo' """, """
          [
            {name: "Kumo", type: "dog"}
          ]
        """, animals)

    @Test
    fun noEscapeValueIsStringAppendExpression() = assertEval("""SELECT * FROM animals as a WHERE a.name || 'xx' LIKE '%xx' """,
                                                             """
          [
            {name: "Kumo", type: "dog"},
            {name:"Mochi",type:"dog"},
            {name:"Lilikoi",type:"unicorn"}
          ]
        """,
                                                             animals)

    @Test
    fun noEscapeValueAndPatternAreBindings() = assertEval("""SELECT a.name FROM
                  `[
                   { name:"Abcd", pattern:"A___" },
                   { name:"100",  pattern:"1%0" }
                  ]` as a
               WHERE a.name LIKE a.pattern """, """
          [
             { name:"Abcd" },
             { name:"100"}
          ]
        """)

    @Test
    fun EscapeLiteralValueAndPatternAreBindings() = assertEval("""SELECT a.name FROM
                  `[
                   { name:"Abcd", pattern:"A___" },
                   { name:"100%",  pattern:"1%0\\%" }
                  ]` as a
               WHERE a.name LIKE a.pattern ESCAPE '\' """, """
          [
             { name:"Abcd" },
             { name:"100%"}
          ]
        """)

    @Test
    fun EscapeValueAndPatternAreBindings() = assertEval("""SELECT a.name FROM
                  `[
                   { name:"Abcd", pattern:"A___" , escapeChar:'['},
                   { name:"100%",  pattern:"1%0[%", escapeChar: '['}
                  ]` as a
               WHERE a.name LIKE a.pattern ESCAPE a.escapeChar """, """
          [
             { name:"Abcd" },
             { name:"100%"}
          ]
        """)

    @Test
    fun NotLikeEscapeValueAndPatternAreBindings() = assertEval("""SELECT a.name FROM
                  `[
                   { name:"Abcd", pattern:"A__" , escapeChar:'['},
                   { name:"1000%",  pattern:"1_0[%", escapeChar: '['}
                  ]` as a
               WHERE a.name NOT LIKE a.pattern ESCAPE a.escapeChar """, """
          [
             { name:"Abcd" },
             { name:"1000%"}
          ]
        """)

    @Test
    fun emptyStringAsEscape() = assertThrows("Cannot use empty character as ESCAPE character in a LIKE predicate: \"\"",
                                             NodeMetadata(1, 51)) {
        voidEval("SELECT * FROM <<>> AS a WHERE '%' LIKE '%' ESCAPE ''")
    }

    @Test
    fun moreThanOneCharacterEscape() = assertThrows("Escape character must have size 1 : []", NodeMetadata(1, 51)) {
        voidEval("SELECT * FROM <<>> AS a WHERE '%' LIKE '%' ESCAPE '[]'")
    }

    @Test
    fun escapeByItself() = assertThrows("Invalid escape sequence : [", NodeMetadata(1, 44)) {
        voidEval("SELECT * FROM <<>> AS a WHERE 'aaaaa' LIKE '[' ESCAPE '['")
    }

    @Test
    fun escapeWithoutWildcard() = assertThrows("Invalid escape sequence : [a", NodeMetadata(1, 44)) {
        voidEval("SELECT * FROM <<>> AS a WHERE 'aaaaa' LIKE '[a' ESCAPE '['")
    }

    @Test
    fun valueNotAString() = assertThrows("LIKE expression must be given non-null strings as input",
                                         NodeMetadata(1, 33)) {
        voidEval("SELECT * FROM <<>> AS a WHERE 1 LIKE 'a' ESCAPE '['")
    }

    @Test
    fun patternNotAString() = assertThrows("LIKE expression must be given non-null strings as input",
                                           NodeMetadata(1, 35)) {
        voidEval("SELECT * FROM <<>> AS a WHERE 'a' LIKE 1 ESCAPE '['")
    }

    @Test
    fun escapeNotAString() = assertThrows("LIKE expression must be given non-null strings as input",
                                          NodeMetadata(1, 35)) {
        // column is marked at the position of LIKE
        voidEval("SELECT * FROM <<>> AS a WHERE 'a' LIKE 'a' ESCAPE 1")
    }

    @Test
    fun valueIsNull() = assertEval("SELECT * FROM <<>> AS a WHERE null LIKE 'a' ESCAPE '['", "[]")

    @Test
    fun patternIsNull() = assertEval("SELECT * FROM <<>> AS a WHERE 'a' LIKE null ESCAPE '['", "[]")

    @Test
    fun escapeIsNull() = assertEval("SELECT * FROM <<>> AS a WHERE 'a' LIKE 'a' ESCAPE null", "[]")

    @Test
    fun nonLiteralsMissingValue() = assertEval("""SELECT * FROM animals as a WHERE a.xxx LIKE '%' """, """
          []
        """, animals)

    @Test
    fun nonLiteralsMissingPattern() = assertEval("""SELECT * FROM animals as a WHERE a.name LIKE a.xxx """, """
          []
        """, animals)

    @Test
    fun nonLiteralsMissingEscape() = assertEval("""SELECT * FROM animals as a WHERE a.name LIKE '%' ESCAPE a.xxx""", """
          []
        """, animals)

    @Test
    fun nonLiteralsNullValue() = assertEval("""SELECT * FROM animalsWithNulls as a WHERE a.name LIKE '%' """, """
          []
        """, animalsWithNulls)

    @Test
    fun nonLiteralsNullPattern() = assertEval("""SELECT * FROM animalsWithNulls as a WHERE a.type LIKE a.name """, """
          []
        """, animalsWithNulls)

    @Test
    fun nonLiteralsNullEscape() = assertEval("""SELECT * FROM animalsWithNulls as a WHERE a.type LIKE '%' ESCAPE a.name""",
                                             """
          []
        """,
                                             animalsWithNulls)


    @Test
    fun nonLiteralsNonStringEscape() = assertThrows("LIKE expression must be given non-null strings as input",
                                                    NodeMetadata(1, 56)) {
        voidEval("SELECT * FROM `[{name:1, type:\"a\"}]` as a WHERE a.type LIKE '%' ESCAPE a.name")
    }

    @Test
    fun nonLiteralsNonStringPattern() = assertThrows("LIKE expression must be given non-null strings as input",
                                                     NodeMetadata(1, 56)) {
        voidEval("SELECT * FROM `[{name:1, type:\"a\"}]` as a WHERE a.type LIKE a.name")
    }

    @Test
    fun nonLiteralsNonStringValue() = assertThrows("LIKE expression must be given non-null strings as input",
                                                   NodeMetadata(1, 56)) {
        voidEval("SELECT * FROM `[{name:1, type:\"a\"}]` as a WHERE a.name LIKE a.type ")
    }

    /** Regression test for: https://github.com/partiql/partiql-lang-kotlin/issues/32 */
    @Test
    fun multiCodepointPattern() = assertEval("'😍' LIKE '😍'", "true")
}
