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

import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.syntax.ParserException
import org.partiql.lang.util.getOffsetHHmm
import java.time.ZoneOffset

class EvaluatingCompilerCastTest : EvaluatorTestBase() {
    private val allTypeNames = ExprValueType.values().flatMap { it.sqlTextNames }

    // cast as NULL is tested by castMissingAsNull
    fun parametersForCastMissing() = allTypeNames - "NULL"

    @Test
    @Parameters
    @TestCaseName("CAST(MISSING AS {0})")
    fun castMissing(typeName: String) {
        assertEval("CAST(MISSING AS $typeName)", "null") {
            assertEquals(ExprValueType.MISSING, exprValue.type)
        }
    }

    @Test
    fun castMissingAsNull() {
        assertEval("CAST(MISSING AS NULL)", "null") {
            assertEquals(ExprValueType.NULL, exprValue.type)
        }
    }

    // cast as MISSING is tested by castNullAsMissing
    fun parametersForCastNull() = allTypeNames - "MISSING"

    @Test
    @Parameters
    @TestCaseName("CAST(NULL AS {0})")
    fun castNull(typeName: String) = assertEval("CAST(NULL AS $typeName)", "null") {
        assertEquals(ExprValueType.NULL, exprValue.type)
    }

    // MISSING values are outputted as "null", see https://github.com/partiql/partiql-lang-kotlin/issues/37
    @Test
    fun castNullAsMissing() = assertEval("CAST(NULL AS MISSING)", "null") {
        assertEquals(ExprValueType.MISSING, exprValue.type)
    }

    /**
     * Test case for general casting tests.
     *
     * @param source The PartiQL expression to cast.
     * @param type The PartiQL type name to cast to.
     * @param expected The expected Ion value of the result, or `null` for [EvaluationException]
     * @param expectedErrorCode The expected error code of any [EvaluationException] or `null` when no exception
     * is to be expected.
     */
    data class CastCase(
            val source: String,
            val type: String,
            val expected: String?,
            val expectedErrorCode: ErrorCode?,
            val session: EvaluationSession = EvaluationSession.standard()
    ) {
        val expression = "CAST($source AS $type)"
        override fun toString(): String = expression
    }

    /** Partial application of the source expression and the expected Ion value without type. */
    fun case(source: String, expected: String?): (String) -> CastCase = {
        CastCase(source, it, expected, null)
    }

    /**
     * Function to create explicit CAST(<source> to <type>) CastCase.
     */
    fun case(source: String, type: String, expected: String) = CastCase(source, type, expected, null)

    /**
     * Function to create explicit CAST(<source> to <type>) CastCase with EvaluationSession.
     */
    fun case(source: String, type: String, expected: String, session: EvaluationSession) = CastCase(source, type, expected, null, session)

    /**
     * Function to create explicit CAST(<source> to <type>) CastCase throwing error.
     */
    fun case(source: String, type: String, expectedErrorCode: ErrorCode) = CastCase(source, type, null, expectedErrorCode)

    /** Partial application of the source expression and the expected error code without type. */
    fun case(source: String, expectedErrorCode: ErrorCode): (String) -> CastCase = {
        CastCase(source, it, null, expectedErrorCode)
    }

    /** For each partial case, apply each of the given types to generate a concrete cast case. */
    fun List<(String) -> CastCase>.types(types: List<String>): List<CastCase> =
        this.flatMap { partial -> types.map { type -> partial(type) } }

    fun parametersForCast() =
            listOf(
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", "false"),
                            case("`true`", "true"),
                            // numbers
                            case("5", "true"),
                            case("`0e0`", "false"),
                            case("1.1", "true"),
                            // timestamp
                            case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("'TrUe'", "true"),
                            case("""`"FALSE"`""", "false"),
                            case("""`'true'`""", "true"),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"goodbye"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"false"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"true"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{Z29vZGJ5ZQ==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // goodbye
                            case("`{{ZmFsc2U=}}`", ErrorCode.EVALUATOR_INVALID_CAST), // false
                            case("`{{dHJ1ZQ==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // true
                            // list
                            case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[true]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[false]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[true, false]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(true)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(false)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{a:true}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{'b':true}", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<true>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<false>>", ErrorCode.EVALUATOR_INVALID_CAST)
                    ).types(ExprValueType.BOOL.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", "0"),
                            case("`true`", "1"),
                            // numbers
                            case("5", "5"),
                            case(" 5 ", "5"),
                            case("`0e0`", "0"),
                            case("1.1", "1"),
                            case("1.9", "1"),
                            case("-20.1", "-20"),
                            case("-20.9", "-20"),
                            // timestamp
                            case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("'1234A'", ErrorCode.EVALUATOR_CAST_FAILED), // Invalid ION value
                            case("'20'", "20"),
                            case("'020'", "20"),
                            case("'+20'", "20"),
                            case("'+020'", "20"),
                            case("'-20'", "-20"),
                            case("'-020'", "-20"),
                            case("'0'", "0"),
                            case("'00'", "0"),
                            case("'+0'", "0"),
                            case("'+00'", "0"),
                            case("'-0'", "0"),
                            case("'-00'", "0"),
                            case("'0xA'", "10"),
                            case("'0XA'", "10"),
                            case("'0x0A'", "10"),
                            case("'+0xA'", "10"),
                            case("'+0x0A'", "10"),
                            case("'-0xA'", "-10"),
                            case("'-0x0A'", "-10"),
                            case("'0b10'", "2"),
                            case("'0B10'", "2"),
                            case("'0b010'", "2"),
                            case("'+0b10'", "2"),
                            case("'+0b010'", "2"),
                            case("'-0b10'", "-2"),
                            case("'-0b010'", "-2"),
                            case("""`"1000"`""", "1000"),
                            case("""`'2e100'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("""`'2d100'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("'00xA'", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("'00b10'", ErrorCode.EVALUATOR_CAST_FAILED),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[1]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[-2, 0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(1)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{a:12}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{'b':-4}", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<14>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<20>>", ErrorCode.EVALUATOR_INVALID_CAST)
                    ).types(ExprValueType.INT.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", "0e0"),
                            case("`true`", "1e0"),
                            // numbers
                            case("5", "5e0"),
                            case(" 5 ", "5e0"),
                            case("`0e0`", "0e0"),
                            case("1.1", "1.1e0"),
                            case("-20.1", "-20.1e0"),
                            // timestamp
                            case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("'-20'", "-20e0"),
                            case("""`"1000"`""", "1000e0"),
                            case("""`'2e100'`""", "2e100"),
                            case("""`'2d100'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[1e0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[-2e0, 0e0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(1e0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(0e0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{a:12e0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{'b':`-4e0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`14e0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`20e0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                    ).types(ExprValueType.FLOAT.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", "0d0"),
                            case("`true`", "1d0"),
                            // numbers
                            case("5", "5d0"),
                            case("5 ", "5d0"),
                            case("`0e0`", "0."),  // TODO formalize this behavior
                            case("`1e0`", "1."),  // TODO formalize this behavior
                            case("1.1", "1.1d0"),
                            case("-20.1", "-20.1d0"),
                            // timestamp
                            case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("'-20'", "-20d0"),
                            case("""`"1000"`""", "1000d0"),
                            case("""`'2e100'`""", "2d100"),
                            case("""`'2d100'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[1d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                    ).types(ExprValueType.DECIMAL.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", "\$partiql_date::2007-10-10"),
                            case("`2007-02-23T12:14Z`", "\$partiql_date::2007-02-23"),
                            case("`2007-02-23T12:14:33.079Z`", "\$partiql_date::2007-02-23"),
                            case("`2007-02-23T12:14:33.079-08:00`", "\$partiql_date::2007-02-23"),
                            case("`2007-02T`", "\$partiql_date::2007-02-01"),
                            case("`2007T`", "\$partiql_date::2007-01-01"),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("'2016-03-01T01:12:12Z'", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("""`"2001-01-01"`""", "\$partiql_date::2001-01-01"),
                            case("""`"+20212-02-01"`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("""`"20212-02-01"`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("""`'2000T'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("""`'1999-04T'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // list
                            case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST)
                    ).types(ExprValueType.DATE.sqlTextNames),
                    // Find more coverage for the "Cast as Time" tests in `castDateAndTime`.
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", "\$partiql_time::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}"),
                            case("`2007-02-23T12:14Z`", "\$partiql_time::{hour:12,minute:14,second:0.,timezone_hour:null.int,timezone_minute:null.int}"),
                            case("`2007-02-23T12:14:33.079Z`", "\$partiql_time::{hour:12,minute:14,second:33.079,timezone_hour:null.int,timezone_minute:null.int}"),
                            case("`2007-02-23T12:14:33.079-08:00`", "\$partiql_time::{hour:12,minute:14,second:33.079,timezone_hour:null.int,timezone_minute:null.int}"),
                            case("`2007-02T`", "\$partiql_time::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}"),
                            case("`2007T`", "\$partiql_time::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}"),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("'2016-03-01T01:12:12Z'", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("""`"23:2:12.12345"`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("""`"+20212-02-01"`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("""`"20212-02-01"`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("""`'2000T'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("""`'1999-04T'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // list
                            case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST)
                    ).types(ExprValueType.TIME.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", "2007-10-10T"),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                            case("'2016-03-01T01:12:12Z'", "2016-03-01T01:12:12Z"),
                            case("""`"2001-01-01"`""", "2001-01-01T"),
                            case("""`'2000T'`""", "2000T"),
                            case("""`'1999-04T'`""", "1999-04T"),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // list
                            case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST)
                    ).types(ExprValueType.TIMESTAMP.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", "'false'"),
                            case("`true`", "'true'"),
                            // numbers
                            case("5", "'5'"),
                            case("`0e0`", "'0.0'"),
                            case("1.1", "'1.1'"),
                            case("-20.1", "'-20.1'"),
                            // timestamp
                            case("`2007-10-10T`", "'2007-10-10'"),
                            // text
                            case("'hello'", "'hello'"),
                            case("'-20'", "'-20'"),
                            case("""`"1000"`""", "'1000'"),
                            case("""`'2e100'`""", "'2e100'"),
                            case("""`'2d100'`""", "'2d100'"),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 0
                            case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 1.0
                            case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 2e10
                            // list
                            case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("['hello']", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                    ).types(ExprValueType.SYMBOL.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", "\"false\""),
                            case("`true`", "\"true\""),
                            // numbers
                            case("5", "\"5\""),
                            case("`0e0`", "\"0.0\""),
                            case("1.1", "\"1.1\""),
                            case("-20.1", "\"-20.1\""),
                            // timestamp
                            case("`2007-10-10T`", "\"2007-10-10\""),
                            // text
                            case("'hello'", "\"hello\""),
                            case("'-20'", "\"-20\""),
                            case("""`"1000"`""", "\"1000\""),
                            case("""`'2e100'`""", "\"2e100\""),
                            case("""`'2d100'`""", "\"2d100\""),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 0
                            case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 1.0
                            case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 2e10
                            // list
                            case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("['hello']", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<'a', <<'hello'>>>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                    ).types(ExprValueType.STRING.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("'-20'", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`"1000"`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`'2e100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`'2d100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            // lob
                            case("""`{{""}}`""", """{{""}}"""),
                            case("""`{{"0"}}`""", """{{"0"}}"""),
                            case("""`{{"1.0"}}`""", """{{"1.0"}}"""),
                            case("""`{{"2e10"}}`""", """{{"2e10"}}"""),
                            case("`{{}}`", """{{""}}"""),
                            case("`{{MA==}}`", """{{"0"}}"""),
                            case("`{{MS4w}}`", """{{"1.0"}}"""),
                            case("`{{MmUxMA==}}`", """{{"2e10"}}"""),
                            // list
                            case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("['hello']", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                    ).types(ExprValueType.CLOB.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("'-20'", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`"1000"`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`'2e100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`'2d100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            // lob
                            case("""`{{""}}`""", """{{}}"""),
                            case("""`{{"0"}}`""", """{{MA==}}"""),
                            case("""`{{"1.0"}}`""", """{{MS4w}}"""),
                            case("""`{{"2e10"}}`""", """{{MmUxMA==}}"""),
                            case("`{{}}`", """{{}}"""),
                            case("`{{MA==}}`", """{{MA==}}"""),     // 0
                            case("`{{MS4w}}`", """{{MS4w}}"""),     // 1.0
                            case("`{{MmUxMA==}}`", """{{MmUxMA==}}"""), // 2e10
                            // list
                            case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("['hello']", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                    ).types(ExprValueType.BLOB.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("'-20'", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`"1000"`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`'2e100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`'2d100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", "[]"),
                            case("['hello']", "[\"hello\"]"),
                            case("`[-2d0, 0d0]`", "[-2d0, 0d0]"),
                            // sexp
                            case("`()`", "[]"),
                            case("`(1d0)`", "[1d0]"),
                            case("`(0d0)`", "[0d0]"),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", "[]"),      // TODO bag verification
                            case("<<`14d0`>>", "[14d0]"),  // TODO bag verification
                            case("<<`20d0`>>", "[20d0]")   // TODO bag verification
                    ).types(ExprValueType.LIST.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("'-20'", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`"1000"`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`'2e100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`'2d100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", "()"),
                            case("['hello']", "(\"hello\")"),
                            case("`[-2d0, 0d0]`", "(-2d0 0d0)"),
                            // sexp
                            case("`()`", "()"),
                            case("`(1d0)`", "(1d0)"),
                            case("`(0d0)`", "(0d0)"),
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", "()"),
                            case("<<`14d0`>>", "(14d0)"),
                            case("<<`20d0`>>", "(20d0)")
                    ).types(ExprValueType.SEXP.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("'-20'", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`"1000"`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`'2e100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`'2d100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("['hello']", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", "{}"),
                            case("{}", "{}"),
                            case("`{a:12d0}`", "{a:12d0}"),
                            case("{'b':`-4d0`}", "{b:-4d0}"),
                            // bag
                            case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                    ).types(ExprValueType.STRUCT.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("'-20'", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`"1000"`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`'2e100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`'2d100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            // lob
                            case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", "[]"),          // TODO bag verification
                            case("['hello']", "[\"hello\"]"), // TODO bag verification
                            case("`[-2d0, 0d0]`", "[-2d0, 0d0]"), // TODO bag verification
                            // sexp
                            case("`()`", "[]"),          // TODO bag verification
                            case("`(1d0)`", "[1d0]"),       // TODO bag verification
                            case("`(0d0)`", "[0d0]"),       // TODO bag verification
                            // struct
                            case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", "[]"),          // TODO bag verification
                            case("<<`14d0`>>", "[14d0]"),      // TODO bag verification
                            case("<<`20d0`>>", "[20d0]")       // TODO bag verification
                    ).types(ExprValueType.BAG.sqlTextNames)
            ).flatMap { it }

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun cast(castCase: CastCase) = when (castCase.expected) {
        null -> {
            try {
                voidEval(castCase.expression)
                fail("Expected evaluation error")
            } catch (e: EvaluationException) {
                if (castCase.expectedErrorCode == null) {
                    fail("CastCase $castCase did not have an expected value or expected error code.")
                }
                assertEquals(castCase.expectedErrorCode, e.errorCode)
            }
        }
        else -> assertEval(castCase.expression, castCase.expected)
    }

    private val defaultTimezoneOffset = ZoneOffset.UTC
    
    private fun buildSession(hours: Int = 0, minutes: Int = 0) = EvaluationSession.build { defaultTimezoneOffset(ZoneOffset.ofHoursMinutes(hours, minutes)) }

    fun parametersForCastDateAndTime() = listOf(
        listOf(
            case("DATE '2007-10-10'", "2007-10-10")
        ).types(ExprValueType.DATE.sqlTextNames),
        listOf(
            case("DATE '2007-10-10'", "`'2007-10-10'`")
        ).types(ExprValueType.SYMBOL.sqlTextNames),
        listOf(
            case("DATE '2007-10-10'", "'2007-10-10'")
        ).types(ExprValueType.STRING.sqlTextNames),
        listOf(
            // CAST(<TIME> AS <variants of TIME type>)
            case("TIME '23:12:12.1267'", "TIME", "23:12:12.1267"),
            case("TIME '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}"),
            case("TIME '23:12:12.1267+05:30'", "TIME (3)", "23:12:12.127"),
            case("TIME '23:12:12.1267-05:30'", "TIME (3) WITH TIME ZONE", "23:12:12.127${defaultTimezoneOffset.getOffsetHHmm()}"),
            case("TIME (3) '23:12:12.1267'", "TIME", "23:12:12.127"),
            case("TIME (3) '23:12:12.1267-05:30'", "TIME", "23:12:12.127"),
            case("TIME (3) '23:12:12.1267+05:30'", "TIME WITH TIME ZONE", "23:12:12.127${defaultTimezoneOffset.getOffsetHHmm()}"),
            case("TIME (3) '23:12:12.1267-05:30'", "TIME (9)", "23:12:12.127000000"),
            case("TIME WITH TIME ZONE '23:12:12.1267'", "TIME", "23:12:12.1267"),
            case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "23:12:12.1267-05:30"),
            case("TIME WITH TIME ZONE '23:12:12.1267+05:30'", "TIME (3) WITH TIME ZONE", "23:12:12.127+05:30"),
            case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "TIME", "23:12:12.1267"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267'", "TIME", "23:12:12.127"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "23:12:12.127-05:30"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267+05:30'", "TIME (5)", "23:12:12.12700"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "TIME (5) WITH TIME ZONE", "23:12:12.12700-05:30"),
            case("TIME '23:12:12.1267'", "TIME", "23:12:12.1267"),
            // CAST(<TIMESTAMP> AS <variants of TIME type>)
            case("`2007-02-23T12:14:33.079Z`", "TIME", "12:14:33.079"),
            case("`2007-02-23T12:14:33.079-08:00`", "TIME", "12:14:33.079"),
            case("`2007-02-23T12:14:33.079Z`", "TIME WITH TIME ZONE", "12:14:33.079+00:00"),
            case("`2007-02-23T12:14:33.079-08:00`", "TIME (1)", "12:14:33.1"),
            case("`2007-02-23T12:14:33.079-08:00`", "TIME (2) WITH TIME ZONE", "12:14:33.08-08:00"),
            // CAST(<text> AS <variants of TIME type>)
            case("'23:12:12.1267'", "TIME", "23:12:12.1267"),
            case("'23:12:12.1267'", "TIME (2)", "23:12:12.13"),
            case("'23:12:12.1267'", "TIME WITH TIME ZONE", "23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}"),
            case("'23:12:12.1267'", "TIME (2) WITH TIME ZONE", "23:12:12.13${defaultTimezoneOffset.getOffsetHHmm()}"),
            case("""`"23:12:12.1267"`""", "TIME", "23:12:12.1267"),
            case("""`"23:12:12.1267"`""", "TIME (2)", "23:12:12.13"),
            case("""`"23:12:12.1267"`""", "TIME WITH TIME ZONE", "23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}"),
            case("""`'23:12:12.1267'`""", "TIME (2) WITH TIME ZONE", "23:12:12.13${defaultTimezoneOffset.getOffsetHHmm()}"),
            case("""`'23:12:12.1267'`""", "TIME", "23:12:12.1267"),
            case("""`'23:12:12.1267'`""", "TIME (2)", "23:12:12.13"),
            case("""`'23:12:12.1267'`""", "TIME WITH TIME ZONE", "23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}"),
            case("""`'23:12:12.1267'`""", "TIME (2) WITH TIME ZONE", "23:12:12.13${defaultTimezoneOffset.getOffsetHHmm()}")
        ),
        // Configuring default timezone offset through EvaluationSession
        listOf(
            // CAST(<TIME> AS <TIME WITH TIME ZONE>)
            case("TIME '23:12:12.1267'", "TIME WITH TIME ZONE", "23:12:12.1267+00:00", buildSession()),
            case("TIME '23:12:12.1267'", "TIME WITH TIME ZONE", "23:12:12.1267+11:00", buildSession(11)),
            case("TIME '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "23:12:12.1267+01:00", buildSession(1)),
            case("TIME '23:12:12.1267-05:30'", "TIME (2) WITH TIME ZONE", "23:12:12.13-05:30", buildSession(-5, -30)),
            // CAST(<TIMESTAMP> AS <TIME WITH TIME ZONE>)
            case("`2007-02-23T12:14:33.079Z`", "TIME WITH TIME ZONE", "12:14:33.079+00:00", buildSession()),
            case("`2007-02-23T12:14:33.079Z`", "TIME WITH TIME ZONE", "12:14:33.079+00:00", buildSession(11)),
            case("`2007-02-23T12:14:33.079-05:30`", "TIME WITH TIME ZONE", "12:14:33.079-05:30", buildSession(1)),
            case("`2007-02-23T12:14:33.079Z`", "TIME (2) WITH TIME ZONE", "12:14:33.08+00:00", buildSession(-5, -30)),
            // CAST(<text> AS <TIME WITH TIME ZONE>)
            case("'23:12:12.1267'", "TIME WITH TIME ZONE", "23:12:12.1267+00:00", buildSession()),
            case("'23:12:12.1267'", "TIME WITH TIME ZONE", "23:12:12.1267+11:00", buildSession(11)),
            case("'23:12:12.1267'", "TIME (2) WITH TIME ZONE", "23:12:12.13-05:30", buildSession(-5, -30)),
            case("""`'23:12:12.1267'`""", "TIME WITH TIME ZONE", "23:12:12.1267+00:00", buildSession()),
            case("""`'23:12:12.1267'`""", "TIME WITH TIME ZONE", "23:12:12.1267+11:00", buildSession(11)),
            case("""`'23:12:12.1267'`""", "TIME (2) WITH TIME ZONE", "23:12:12.13-05:30", buildSession(-5, -30)),
            case("""`"23:12:12.1267"`""", "TIME WITH TIME ZONE", "23:12:12.1267+00:00", buildSession()),
            case("""`"23:12:12.1267"`""", "TIME WITH TIME ZONE", "23:12:12.1267+11:00", buildSession(11)),
            case("""`"23:12:12.1267"`""", "TIME (2) WITH TIME ZONE", "23:12:12.13-05:30", buildSession(-5, -30))
        ),
        // Error cases for TIME
        listOf(
            case("TIME '23:12:12.1267'", "TIME (-1)", ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME),
            case("TIME '23:12:12.1267'", "TIME (1, 2)", ErrorCode.PARSE_CAST_ARITY),
            case("TIME '23:12:12.1267'", "TIME (1, 2) WITH TIME ZONE", ErrorCode.PARSE_CAST_ARITY),
            case("TIME '23:12:12.1267-05:30'", "TIME (10) WITH TIME ZONE", ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME),
            case("TIME '23:12:12.1267+05:30'", "TIME (10)", ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME),
            // Cannot case timestamp with undefined timezone to "TIME WITH TIME ZONE"
            case("`2007-02-23T`", "TIME WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
            case("`2007-02-23T12:14:33.079-00:00`", "TIME WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
            // Invalid format for time text.
            case("'23:12:2.1267'", "TIME", ErrorCode.EVALUATOR_CAST_FAILED),
            case("'2:12:2.1267'", "TIME (2)", ErrorCode.EVALUATOR_CAST_FAILED),
            case("'25:12:2.1267'", "TIME WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
            case("'24:60:2.1267'", "TIME (2) WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
            case("""`"12:60:12.1267"`""", "TIME", ErrorCode.EVALUATOR_CAST_FAILED),
            case("""`"23:1:12.1267"`""", "TIME (2)", ErrorCode.EVALUATOR_CAST_FAILED),
            case("""`"30:12:12.1267"`""", "TIME WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
            case("""`'23:12:60.1267'`""", "TIME (2) WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
            case("""`'23:12:1.1267'`""", "TIME", ErrorCode.EVALUATOR_CAST_FAILED),
            case("""`'2:12:12.1267'`""", "TIME (2)", ErrorCode.EVALUATOR_CAST_FAILED),
            case("""`'23:1:12.1267'`""", "TIME WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
            case("""`'-23:41:12.1267'`""", "TIME (2) WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED)
        ),

        listOf(
            case("TIME '23:12:12.1267'", "`'23:12:12.1267'`"),
            case("TIME '23:12:12.1267-05:30'", "`'23:12:12.1267'`"),
            case("TIME '23:12:12.1267+05:30'", "`'23:12:12.1267'`"),
            case("TIME '23:12:12.1267-05:30'", "`'23:12:12.1267'`"),
            case("TIME (3) '23:12:12.1267'", "`'23:12:12.127'`"),
            case("TIME (3) '23:12:12.1267-05:30'", "`'23:12:12.127'`"),
            case("TIME (3) '23:12:12.1267+05:30'", "`'23:12:12.127'`"),
            case("TIME (3) '23:12:12.1267-05:30'", "`'23:12:12.127'`"),
            case("TIME WITH TIME ZONE '23:12:12.1267'", "`'23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}'`"),
            case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "`'23:12:12.1267-05:30'`"),
            case("TIME WITH TIME ZONE '23:12:12.1267+05:30'", "`'23:12:12.1267+05:30'`"),
            case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "`'23:12:12.1267-05:30'`"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267'", "`'23:12:12.127${defaultTimezoneOffset.getOffsetHHmm()}'`"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "`'23:12:12.127-05:30'`"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267+05:30'", "`'23:12:12.127+05:30'`"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "`'23:12:12.127-05:30'`")
        ).types(ExprValueType.SYMBOL.sqlTextNames),
        listOf(
            case("TIME '23:12:12.1267'", "'23:12:12.1267'"),
            case("TIME '23:12:12.1267-05:30'", "'23:12:12.1267'"),
            case("TIME '23:12:12.1267+05:30'", "'23:12:12.1267'"),
            case("TIME '23:12:12.1267-05:30'", "'23:12:12.1267'"),
            case("TIME (3) '23:12:12.1267'", "'23:12:12.127'"),
            case("TIME (3) '23:12:12.1267-05:30'", "'23:12:12.127'"),
            case("TIME (3) '23:12:12.1267+05:30'", "'23:12:12.127'"),
            case("TIME (3) '23:12:12.1267-05:30'", "'23:12:12.127'"),
            case("TIME WITH TIME ZONE '23:12:12.1267'", "'23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}'"),
            case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "'23:12:12.1267-05:30'"),
            case("TIME WITH TIME ZONE '23:12:12.1267+05:30'", "'23:12:12.1267+05:30'"),
            case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "'23:12:12.1267-05:30'"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267'", "'23:12:12.127${defaultTimezoneOffset.getOffsetHHmm()}'"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "'23:12:12.127-05:30'"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267+05:30'", "'23:12:12.127+05:30'"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "'23:12:12.127-05:30'")
        ).types(ExprValueType.STRING.sqlTextNames)
    ).flatten() +
        listOf(
                ExprValueType.MISSING,
                ExprValueType.NULL,
                ExprValueType.BOOL,
                ExprValueType.INT,
                ExprValueType.FLOAT,
                ExprValueType.DECIMAL,
                ExprValueType.TIMESTAMP,
                ExprValueType.CLOB,
                ExprValueType.BLOB,
                ExprValueType.LIST,
                ExprValueType.SEXP,
                ExprValueType.STRUCT,
                ExprValueType.BAG).map {
                listOf(case("DATE '2007-10-10'", ErrorCode.EVALUATOR_INVALID_CAST)).types(it.sqlTextNames)
            }.flatten()

    // Separate tests for Date and Time as [assertEval] validates serialization and
    // date and time literals are not supported by V0 AST serializer.
    @Test
    @Parameters
    @TestCaseName("{0}")
    fun castDateAndTime(castCase: CastCase) = when (castCase.expected) {
        null -> {
            try {
                voidEval(castCase.expression)
                fail("Expected evaluation error")
            } catch (e: EvaluationException) {
                if (castCase.expectedErrorCode == null) {
                    fail("CastCase $castCase did not have an expected value or expected error code.")
                }
                assertEquals(castCase.expectedErrorCode, e.errorCode)
            } catch (p: ParserException) {
                if (castCase.expectedErrorCode == null) {
                    fail("CastCase $castCase did not have an expected value or expected error code.")
                }
                assertEquals(castCase.expectedErrorCode, p.errorCode)
            }
        }
        else -> assertEquals(
            castCase.expected,
            eval(source = castCase.expression, session = castCase.session).toString()
        )
    }
}

