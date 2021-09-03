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

import org.partiql.lang.errors.*
import org.partiql.lang.eval.ExprValueType.*
import org.partiql.lang.errors.ErrorCode.*
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import org.partiql.lang.syntax.ParserException
import org.partiql.lang.util.DEFAULT_TIMEZONE_OFFSET
import org.partiql.lang.util.getOffsetHHmm

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
    data class CastCase(val source: String, val type: String, val expected: String?, val expectedErrorCode: ErrorCode?) {
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
                            case("`2007-10-10T`", EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", EVALUATOR_CAST_FAILED),
                            case("'TrUe'", "true"),
                            case("""`"FALSE"`""", "false"),
                            case("""`'true'`""", "true"),
                            // lob
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"goodbye"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"false"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"true"}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            case("`{{Z29vZGJ5ZQ==}}`", EVALUATOR_INVALID_CAST), // goodbye
                            case("`{{ZmFsc2U=}}`", EVALUATOR_INVALID_CAST), // false
                            case("`{{dHJ1ZQ==}}`", EVALUATOR_INVALID_CAST), // true
                            // list
                            case("`[]`", EVALUATOR_INVALID_CAST),
                            case("`[true]`", EVALUATOR_INVALID_CAST),
                            case("`[false]`", EVALUATOR_INVALID_CAST),
                            case("`[true, false]`", EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", EVALUATOR_INVALID_CAST),
                            case("`(true)`", EVALUATOR_INVALID_CAST),
                            case("`(false)`", EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            case("{}", EVALUATOR_INVALID_CAST),
                            case("`{a:true}`", EVALUATOR_INVALID_CAST),
                            case("{'b':true}", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", EVALUATOR_INVALID_CAST),
                            case("<<true>>", EVALUATOR_INVALID_CAST),
                            case("<<false>>", EVALUATOR_INVALID_CAST)
                    ).types(BOOL.sqlTextNames),
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
                            case("`2007-10-10T`", EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", EVALUATOR_CAST_FAILED),
                            case("'1234A'", EVALUATOR_CAST_FAILED), // Invalid ION value
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
                            case("""`'2e100'`""", EVALUATOR_CAST_FAILED),
                            case("""`'2d100'`""", EVALUATOR_CAST_FAILED),
                            case("'00xA'", EVALUATOR_CAST_FAILED),
                            case("'00b10'", EVALUATOR_CAST_FAILED),
                            // lob
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", EVALUATOR_INVALID_CAST),
                            case("`[1]`", EVALUATOR_INVALID_CAST),
                            case("`[-2, 0]`", EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", EVALUATOR_INVALID_CAST),
                            case("`(1)`", EVALUATOR_INVALID_CAST),
                            case("`(0)`", EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            case("{}", EVALUATOR_INVALID_CAST),
                            case("`{a:12}`", EVALUATOR_INVALID_CAST),
                            case("{'b':-4}", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", EVALUATOR_INVALID_CAST),
                            case("<<14>>", EVALUATOR_INVALID_CAST),
                            case("<<20>>", EVALUATOR_INVALID_CAST)
                    ).types(INT.sqlTextNames),
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
                            case("`2007-10-10T`", EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", EVALUATOR_CAST_FAILED),
                            case("'-20'", "-20e0"),
                            case("""`"1000"`""", "1000e0"),
                            case("""`'2e100'`""", "2e100"),
                            case("""`'2d100'`""", EVALUATOR_CAST_FAILED),
                            // lob
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", EVALUATOR_INVALID_CAST),
                            case("`[1e0]`", EVALUATOR_INVALID_CAST),
                            case("`[-2e0, 0e0]`", EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", EVALUATOR_INVALID_CAST),
                            case("`(1e0)`", EVALUATOR_INVALID_CAST),
                            case("`(0e0)`", EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            case("{}", EVALUATOR_INVALID_CAST),
                            case("`{a:12e0}`", EVALUATOR_INVALID_CAST),
                            case("{'b':`-4e0`}", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", EVALUATOR_INVALID_CAST),
                            case("<<`14e0`>>", EVALUATOR_INVALID_CAST),
                            case("<<`20e0`>>", EVALUATOR_INVALID_CAST)
                    ).types(FLOAT.sqlTextNames),
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
                            case("`2007-10-10T`", EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", EVALUATOR_CAST_FAILED),
                            case("'-20'", "-20d0"),
                            case("""`"1000"`""", "1000d0"),
                            case("""`'2e100'`""", "2d100"),
                            case("""`'2d100'`""", EVALUATOR_CAST_FAILED),
                            // lob
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", EVALUATOR_INVALID_CAST),
                            case("`[1d0]`", EVALUATOR_INVALID_CAST),
                            case("`[-2d0, 0d0]`", EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", EVALUATOR_INVALID_CAST),
                            case("`(1d0)`", EVALUATOR_INVALID_CAST),
                            case("`(0d0)`", EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            case("{}", EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", EVALUATOR_INVALID_CAST),
                            case("<<`14d0`>>", EVALUATOR_INVALID_CAST),
                            case("<<`20d0`>>", EVALUATOR_INVALID_CAST)
                    ).types(DECIMAL.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", EVALUATOR_INVALID_CAST),
                            case("`true`", EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", EVALUATOR_INVALID_CAST),
                            case("`0e0`", EVALUATOR_INVALID_CAST),
                            case("1.1", EVALUATOR_INVALID_CAST),
                            case("-20.1", EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", "\$partiql_date::2007-10-10"),
                            case("`2007-02-23T12:14Z`", "\$partiql_date::2007-02-23"),
                            case("`2007-02-23T12:14:33.079Z`", "\$partiql_date::2007-02-23"),
                            case("`2007-02-23T12:14:33.079-08:00`", "\$partiql_date::2007-02-23"),
                            case("`2007-02T`", "\$partiql_date::2007-02-01"),
                            case("`2007T`", "\$partiql_date::2007-01-01"),
                            // text
                            case("'hello'", EVALUATOR_CAST_FAILED),
                            case("'2016-03-01T01:12:12Z'", EVALUATOR_CAST_FAILED),
                            case("""`"2001-01-01"`""", "\$partiql_date::2001-01-01"),
                            case("""`"+20212-02-01"`""", EVALUATOR_CAST_FAILED),
                            case("""`"20212-02-01"`""", EVALUATOR_CAST_FAILED),
                            case("""`'2000T'`""", EVALUATOR_CAST_FAILED),
                            case("""`'1999-04T'`""", EVALUATOR_CAST_FAILED),
                            // lob
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            // list
                            case("`[]`", EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", EVALUATOR_INVALID_CAST)
                    ).types(DATE.sqlTextNames),
                   // Find more coverage for the "Cast as Time" tests in `castDateAndTime`.
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", EVALUATOR_INVALID_CAST),
                            case("`true`", EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", EVALUATOR_INVALID_CAST),
                            case("`0e0`", EVALUATOR_INVALID_CAST),
                            case("1.1", EVALUATOR_INVALID_CAST),
                            case("-20.1", EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", "\$partiql_time::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}"),
                            case("`2007-02-23T12:14Z`", "\$partiql_time::{hour:12,minute:14,second:0.,timezone_hour:null.int,timezone_minute:null.int}"),
                            case("`2007-02-23T12:14:33.079Z`", "\$partiql_time::{hour:12,minute:14,second:33.079,timezone_hour:null.int,timezone_minute:null.int}"),
                            case("`2007-02-23T12:14:33.079-08:00`", "\$partiql_time::{hour:12,minute:14,second:33.079,timezone_hour:null.int,timezone_minute:null.int}"),
                            case("`2007-02T`", "\$partiql_time::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}"),
                            case("`2007T`", "\$partiql_time::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}"),
                            // text
                            case("'hello'", EVALUATOR_CAST_FAILED),
                            case("'2016-03-01T01:12:12Z'", EVALUATOR_CAST_FAILED),
                            case("""`"23:2:12.12345"`""", EVALUATOR_CAST_FAILED),
                            case("""`"+20212-02-01"`""", EVALUATOR_CAST_FAILED),
                            case("""`"20212-02-01"`""", EVALUATOR_CAST_FAILED),
                            case("""`'2000T'`""", EVALUATOR_CAST_FAILED),
                            case("""`'1999-04T'`""", EVALUATOR_CAST_FAILED),
                            // lob
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            // list
                            case("`[]`", EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", EVALUATOR_INVALID_CAST)
                    ).types(TIME.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", EVALUATOR_INVALID_CAST),
                            case("`true`", EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", EVALUATOR_INVALID_CAST),
                            case("`0e0`", EVALUATOR_INVALID_CAST),
                            case("1.1", EVALUATOR_INVALID_CAST),
                            case("-20.1", EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", "2007-10-10T"),
                            // text
                            case("'hello'", EVALUATOR_CAST_FAILED),
                            case("'2016-03-01T01:12:12Z'", "2016-03-01T01:12:12Z"),
                            case("""`"2001-01-01"`""", "2001-01-01T"),
                            case("""`'2000T'`""", "2000T"),
                            case("""`'1999-04T'`""", "1999-04T"),
                            // lob
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            // list
                            case("`[]`", EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", EVALUATOR_INVALID_CAST)
                    ).types(TIMESTAMP.sqlTextNames),
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
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", EVALUATOR_INVALID_CAST),  // 0
                            case("`{{MS4w}}`", EVALUATOR_INVALID_CAST),  // 1.0
                            case("`{{MmUxMA==}}`", EVALUATOR_INVALID_CAST),  // 2e10
                            // list
                            case("`[]`", EVALUATOR_INVALID_CAST),
                            case("['hello']", EVALUATOR_INVALID_CAST),
                            case("`[-2d0, 0d0]`", EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", EVALUATOR_INVALID_CAST),
                            case("`(1d0)`", EVALUATOR_INVALID_CAST),
                            case("`(0d0)`", EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            case("{}", EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", EVALUATOR_INVALID_CAST),
                            case("<<`14d0`>>", EVALUATOR_INVALID_CAST),
                            case("<<`20d0`>>", EVALUATOR_INVALID_CAST)
                    ).types(SYMBOL.sqlTextNames),
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
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", EVALUATOR_INVALID_CAST),  // 0
                            case("`{{MS4w}}`", EVALUATOR_INVALID_CAST),  // 1.0
                            case("`{{MmUxMA==}}`", EVALUATOR_INVALID_CAST),  // 2e10
                            // list
                            case("`[]`", EVALUATOR_INVALID_CAST),
                            case("['hello']", EVALUATOR_INVALID_CAST),
                            case("`[-2d0, 0d0]`", EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", EVALUATOR_INVALID_CAST),
                            case("`(1d0)`", EVALUATOR_INVALID_CAST),
                            case("`(0d0)`", EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            case("{}", EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", EVALUATOR_INVALID_CAST),
                            case("<<`14d0`>>", EVALUATOR_INVALID_CAST),
                            case("<<'a', <<'hello'>>>>", EVALUATOR_INVALID_CAST),
                            case("<<`20d0`>>", EVALUATOR_INVALID_CAST)
                    ).types(STRING.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", EVALUATOR_INVALID_CAST),
                            case("`true`", EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", EVALUATOR_INVALID_CAST),
                            case("`0e0`", EVALUATOR_INVALID_CAST),
                            case("1.1", EVALUATOR_INVALID_CAST),
                            case("-20.1", EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", EVALUATOR_INVALID_CAST),
                            case("'-20'", EVALUATOR_INVALID_CAST),
                            case("""`"1000"`""", EVALUATOR_INVALID_CAST),
                            case("""`'2e100'`""", EVALUATOR_INVALID_CAST),
                            case("""`'2d100'`""", EVALUATOR_INVALID_CAST),
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
                            case("`[]`", EVALUATOR_INVALID_CAST),
                            case("['hello']", EVALUATOR_INVALID_CAST),
                            case("`[-2d0, 0d0]`", EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", EVALUATOR_INVALID_CAST),
                            case("`(1d0)`", EVALUATOR_INVALID_CAST),
                            case("`(0d0)`", EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            case("{}", EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", EVALUATOR_INVALID_CAST),
                            case("<<`14d0`>>", EVALUATOR_INVALID_CAST),
                            case("<<`20d0`>>", EVALUATOR_INVALID_CAST)
                    ).types(CLOB.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", EVALUATOR_INVALID_CAST),
                            case("`true`", EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", EVALUATOR_INVALID_CAST),
                            case("`0e0`", EVALUATOR_INVALID_CAST),
                            case("1.1", EVALUATOR_INVALID_CAST),
                            case("-20.1", EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", EVALUATOR_INVALID_CAST),
                            case("'-20'", EVALUATOR_INVALID_CAST),
                            case("""`"1000"`""", EVALUATOR_INVALID_CAST),
                            case("""`'2e100'`""", EVALUATOR_INVALID_CAST),
                            case("""`'2d100'`""", EVALUATOR_INVALID_CAST),
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
                            case("`[]`", EVALUATOR_INVALID_CAST),
                            case("['hello']", EVALUATOR_INVALID_CAST),
                            case("`[-2d0, 0d0]`", EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", EVALUATOR_INVALID_CAST),
                            case("`(1d0)`", EVALUATOR_INVALID_CAST),
                            case("`(0d0)`", EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            case("{}", EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", EVALUATOR_INVALID_CAST),
                            case("<<`14d0`>>", EVALUATOR_INVALID_CAST),
                            case("<<`20d0`>>", EVALUATOR_INVALID_CAST)
                    ).types(BLOB.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", EVALUATOR_INVALID_CAST),
                            case("`true`", EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", EVALUATOR_INVALID_CAST),
                            case("`0e0`", EVALUATOR_INVALID_CAST),
                            case("1.1", EVALUATOR_INVALID_CAST),
                            case("-20.1", EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", EVALUATOR_INVALID_CAST),
                            case("'-20'", EVALUATOR_INVALID_CAST),
                            case("""`"1000"`""", EVALUATOR_INVALID_CAST),
                            case("""`'2e100'`""", EVALUATOR_INVALID_CAST),
                            case("""`'2d100'`""", EVALUATOR_INVALID_CAST),
                            // lob
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", "[]"),
                            case("['hello']", "[\"hello\"]"),
                            case("`[-2d0, 0d0]`", "[-2d0, 0d0]"),
                            // sexp
                            case("`()`", "[]"),
                            case("`(1d0)`", "[1d0]"),
                            case("`(0d0)`", "[0d0]"),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            case("{}", EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", "[]"),      // TODO bag verification
                            case("<<`14d0`>>", "[14d0]"),  // TODO bag verification
                            case("<<`20d0`>>", "[20d0]")   // TODO bag verification
                    ).types(LIST.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", EVALUATOR_INVALID_CAST),
                            case("`true`", EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", EVALUATOR_INVALID_CAST),
                            case("`0e0`", EVALUATOR_INVALID_CAST),
                            case("1.1", EVALUATOR_INVALID_CAST),
                            case("-20.1", EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", EVALUATOR_INVALID_CAST),
                            case("'-20'", EVALUATOR_INVALID_CAST),
                            case("""`"1000"`""", EVALUATOR_INVALID_CAST),
                            case("""`'2e100'`""", EVALUATOR_INVALID_CAST),
                            case("""`'2d100'`""", EVALUATOR_INVALID_CAST),
                            // lob
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", "()"),
                            case("['hello']", "(\"hello\")"),
                            case("`[-2d0, 0d0]`", "(-2d0 0d0)"),
                            // sexp
                            case("`()`", "()"),
                            case("`(1d0)`", "(1d0)"),
                            case("`(0d0)`", "(0d0)"),
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            case("{}", EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", "()"),
                            case("<<`14d0`>>", "(14d0)"),
                            case("<<`20d0`>>", "(20d0)")
                    ).types(SEXP.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", EVALUATOR_INVALID_CAST),
                            case("`true`", EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", EVALUATOR_INVALID_CAST),
                            case("`0e0`", EVALUATOR_INVALID_CAST),
                            case("1.1", EVALUATOR_INVALID_CAST),
                            case("-20.1", EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", EVALUATOR_INVALID_CAST),
                            case("'-20'", EVALUATOR_INVALID_CAST),
                            case("""`"1000"`""", EVALUATOR_INVALID_CAST),
                            case("""`'2e100'`""", EVALUATOR_INVALID_CAST),
                            case("""`'2d100'`""", EVALUATOR_INVALID_CAST),
                            // lob
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", EVALUATOR_INVALID_CAST),
                            case("['hello']", EVALUATOR_INVALID_CAST),
                            case("`[-2d0, 0d0]`", EVALUATOR_INVALID_CAST),
                            // sexp
                            case("`()`", EVALUATOR_INVALID_CAST),
                            case("`(1d0)`", EVALUATOR_INVALID_CAST),
                            case("`(0d0)`", EVALUATOR_INVALID_CAST),
                            // struct
                            case("`{}`", "{}"),
                            case("{}", "{}"),
                            case("`{a:12d0}`", "{a:12d0}"),
                            case("{'b':`-4d0`}", "{b:-4d0}"),
                            // bag
                            case("<<>>", EVALUATOR_INVALID_CAST),
                            case("<<`14d0`>>", EVALUATOR_INVALID_CAST),
                            case("<<`20d0`>>", EVALUATOR_INVALID_CAST)
                    ).types(STRUCT.sqlTextNames),
                    listOf(
                            // booleans
                            case("TRUE AND FALSE", EVALUATOR_INVALID_CAST),
                            case("`true`", EVALUATOR_INVALID_CAST),
                            // numbers
                            case("5", EVALUATOR_INVALID_CAST),
                            case("`0e0`", EVALUATOR_INVALID_CAST),
                            case("1.1", EVALUATOR_INVALID_CAST),
                            case("-20.1", EVALUATOR_INVALID_CAST),
                            // timestamp
                            case("`2007-10-10T`", EVALUATOR_INVALID_CAST),
                            // text
                            case("'hello'", EVALUATOR_INVALID_CAST),
                            case("'-20'", EVALUATOR_INVALID_CAST),
                            case("""`"1000"`""", EVALUATOR_INVALID_CAST),
                            case("""`'2e100'`""", EVALUATOR_INVALID_CAST),
                            case("""`'2d100'`""", EVALUATOR_INVALID_CAST),
                            // lob
                            case("""`{{""}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"1.0"}}`""", EVALUATOR_INVALID_CAST),
                            case("""`{{"2e10"}}`""", EVALUATOR_INVALID_CAST),
                            case("`{{}}`", EVALUATOR_INVALID_CAST),
                            case("`{{MA==}}`", EVALUATOR_INVALID_CAST), // 0
                            case("`{{MS4w}}`", EVALUATOR_INVALID_CAST), // 1.0
                            case("`{{MmUxMA==}}`", EVALUATOR_INVALID_CAST), // 2e10
                            // list
                            case("`[]`", "[]"),          // TODO bag verification
                            case("['hello']", "[\"hello\"]"), // TODO bag verification
                            case("`[-2d0, 0d0]`", "[-2d0, 0d0]"), // TODO bag verification
                            // sexp
                            case("`()`", "[]"),          // TODO bag verification
                            case("`(1d0)`", "[1d0]"),       // TODO bag verification
                            case("`(0d0)`", "[0d0]"),       // TODO bag verification
                            // struct
                            case("`{}`", EVALUATOR_INVALID_CAST),
                            case("{}", EVALUATOR_INVALID_CAST),
                            case("`{a:12d0}`", EVALUATOR_INVALID_CAST),
                            case("{'b':`-4d0`}", EVALUATOR_INVALID_CAST),
                            // bag
                            case("<<>>", "[]"),          // TODO bag verification
                            case("<<`14d0`>>", "[14d0]"),      // TODO bag verification
                            case("<<`20d0`>>", "[20d0]")       // TODO bag verification
                    ).types(BAG.sqlTextNames)
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

    fun parametersForCastDateAndTime() = listOf(
        listOf(
            case("DATE '2007-10-10'", "2007-10-10")
        ).types(DATE.sqlTextNames),
        listOf(
            case("DATE '2007-10-10'", "`'2007-10-10'`")
        ).types(SYMBOL.sqlTextNames),
        listOf(
            case("DATE '2007-10-10'", "'2007-10-10'")
        ).types(STRING.sqlTextNames),
        listOf(
            // CAST(<TIME> AS <variants of TIME type>)
            case("TIME '23:12:12.1267'", "TIME", "23:12:12.1267"),
            case("TIME '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "23:12:12.1267${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}"),
            case("TIME '23:12:12.1267+05:30'", "TIME (3)", "23:12:12.127"),
            case("TIME '23:12:12.1267-05:30'", "TIME (3) WITH TIME ZONE", "23:12:12.127${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}"),
            case("TIME (3) '23:12:12.1267'", "TIME","23:12:12.127"),
            case("TIME (3) '23:12:12.1267-05:30'", "TIME","23:12:12.127"),
            case("TIME (3) '23:12:12.1267+05:30'", "TIME WITH TIME ZONE","23:12:12.127${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}"),
            case("TIME (3) '23:12:12.1267-05:30'", "TIME (9)","23:12:12.127000000"),
            case("TIME WITH TIME ZONE '23:12:12.1267'", "TIME", "23:12:12.1267"),
            case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "23:12:12.1267-05:30"),
            case("TIME WITH TIME ZONE '23:12:12.1267+05:30'", "TIME (3) WITH TIME ZONE","23:12:12.127+05:30"),
            case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "TIME", "23:12:12.1267"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267'", "TIME", "23:12:12.127"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "23:12:12.127-05:30"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267+05:30'", "TIME (5)", "23:12:12.12700"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "TIME (5) WITH TIME ZONE","23:12:12.12700-05:30"),
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
            case("'23:12:12.1267'", "TIME WITH TIME ZONE", "23:12:12.1267${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}"),
            case("'23:12:12.1267'", "TIME (2) WITH TIME ZONE", "23:12:12.13${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}"),
            case("""`"23:12:12.1267"`""", "TIME", "23:12:12.1267"),
            case("""`"23:12:12.1267"`""", "TIME (2)", "23:12:12.13"),
            case("""`"23:12:12.1267"`""", "TIME WITH TIME ZONE", "23:12:12.1267${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}"),
            case("""`'23:12:12.1267'`""", "TIME (2) WITH TIME ZONE", "23:12:12.13${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}"),
            case("""`'23:12:12.1267'`""", "TIME", "23:12:12.1267"),
            case("""`'23:12:12.1267'`""", "TIME (2)", "23:12:12.13"),
            case("""`'23:12:12.1267'`""", "TIME WITH TIME ZONE", "23:12:12.1267${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}"),
            case("""`'23:12:12.1267'`""", "TIME (2) WITH TIME ZONE", "23:12:12.13${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}")
        ),
        // Error cases for TIME
        listOf(
            case("TIME '23:12:12.1267'", "TIME (-1)", PARSE_INVALID_PRECISION_FOR_TIME),
                case("TIME '23:12:12.1267'", "TIME (1, 2)", PARSE_CAST_ARITY),
                case("TIME '23:12:12.1267'", "TIME (1, 2) WITH TIME ZONE", PARSE_CAST_ARITY),
            case("TIME '23:12:12.1267-05:30'", "TIME (10) WITH TIME ZONE", PARSE_INVALID_PRECISION_FOR_TIME),
            case("TIME '23:12:12.1267+05:30'", "TIME (10)", PARSE_INVALID_PRECISION_FOR_TIME),
            // Cannot case timestamp with undefined timezone to "TIME WITH TIME ZONE"
            case("`2007-02-23T`", "TIME WITH TIME ZONE", EVALUATOR_CAST_FAILED),
            case("`2007-02-23T12:14:33.079-00:00`", "TIME WITH TIME ZONE", EVALUATOR_CAST_FAILED),
            // Invalid format for time text.
            case("'23:12:2.1267'", "TIME", EVALUATOR_CAST_FAILED),
            case("'2:12:2.1267'", "TIME (2)", EVALUATOR_CAST_FAILED),
            case("'25:12:2.1267'", "TIME WITH TIME ZONE", EVALUATOR_CAST_FAILED),
            case("'24:60:2.1267'", "TIME (2) WITH TIME ZONE", EVALUATOR_CAST_FAILED),
            case("""`"12:60:12.1267"`""", "TIME", EVALUATOR_CAST_FAILED),
            case("""`"23:1:12.1267"`""", "TIME (2)", EVALUATOR_CAST_FAILED),
            case("""`"30:12:12.1267"`""", "TIME WITH TIME ZONE", EVALUATOR_CAST_FAILED),
            case("""`'23:12:60.1267'`""", "TIME (2) WITH TIME ZONE", EVALUATOR_CAST_FAILED),
            case("""`'23:12:1.1267'`""", "TIME", EVALUATOR_CAST_FAILED),
            case("""`'2:12:12.1267'`""", "TIME (2)", EVALUATOR_CAST_FAILED),
            case("""`'23:1:12.1267'`""", "TIME WITH TIME ZONE", EVALUATOR_CAST_FAILED),
            case("""`'-23:41:12.1267'`""", "TIME (2) WITH TIME ZONE", EVALUATOR_CAST_FAILED)
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
            case("TIME WITH TIME ZONE '23:12:12.1267'", "`'23:12:12.1267${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}'`"),
            case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "`'23:12:12.1267-05:30'`"),
            case("TIME WITH TIME ZONE '23:12:12.1267+05:30'", "`'23:12:12.1267+05:30'`"),
            case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "`'23:12:12.1267-05:30'`"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267'", "`'23:12:12.127${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}'`"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "`'23:12:12.127-05:30'`"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267+05:30'", "`'23:12:12.127+05:30'`"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "`'23:12:12.127-05:30'`")
        ).types(SYMBOL.sqlTextNames),
        listOf(
            case("TIME '23:12:12.1267'", "'23:12:12.1267'"),
            case("TIME '23:12:12.1267-05:30'", "'23:12:12.1267'"),
            case("TIME '23:12:12.1267+05:30'", "'23:12:12.1267'"),
            case("TIME '23:12:12.1267-05:30'", "'23:12:12.1267'"),
            case("TIME (3) '23:12:12.1267'", "'23:12:12.127'"),
            case("TIME (3) '23:12:12.1267-05:30'", "'23:12:12.127'"),
            case("TIME (3) '23:12:12.1267+05:30'", "'23:12:12.127'"),
            case("TIME (3) '23:12:12.1267-05:30'", "'23:12:12.127'"),
            case("TIME WITH TIME ZONE '23:12:12.1267'", "'23:12:12.1267${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}'"),
            case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "'23:12:12.1267-05:30'"),
            case("TIME WITH TIME ZONE '23:12:12.1267+05:30'", "'23:12:12.1267+05:30'"),
            case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "'23:12:12.1267-05:30'"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267'", "'23:12:12.127${DEFAULT_TIMEZONE_OFFSET.getOffsetHHmm()}'"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "'23:12:12.127-05:30'"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267+05:30'", "'23:12:12.127+05:30'"),
            case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "'23:12:12.127-05:30'")
        ).types(STRING.sqlTextNames)
    ).flatten() +
        listOf(MISSING, NULL, BOOL, INT, FLOAT, DECIMAL, TIMESTAMP, CLOB, BLOB, LIST, SEXP, STRUCT, BAG)
        .map { listOf(case("DATE '2007-10-10'", EVALUATOR_INVALID_CAST)).types(it.sqlTextNames)
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
        else -> assertEquals(castCase.expected, eval(castCase.expression).toString())
    }
}

