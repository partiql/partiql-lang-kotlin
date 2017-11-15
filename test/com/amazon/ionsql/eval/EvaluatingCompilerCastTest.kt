/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.ExprValueType.*
import com.amazon.ionsql.errors.ErrorCode.*
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test

class EvaluatingCompilerCastTest : EvaluatorBase() {
    val allTypeNames = ExprValueType.values().flatMap { it.sqlTextNames }

    fun parametersForCastMissing() = allTypeNames
    @Test
    @Parameters
    @TestCaseName("CAST(MISSING AS {0})")
    fun castMissing(typeName: String) = assertEval("CAST(MISSING AS $typeName)", "null") {
        assertEquals(ExprValueType.MISSING, exprValue.type)
    }

    fun parametersForCastNull() = allTypeNames
    @Test
    @Parameters
    @TestCaseName("CAST(NULL AS {0})")
    fun castNull(typeName: String) = assertEval("CAST(NULL AS $typeName)", "null") {
        assertEquals(ExprValueType.NULL, exprValue.type)
    }

    /**
     * Test case for general casting tests.
     *
     * @param source The SQL++ expression to cast.
     * @param type The SQL++ type name to cast to.
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
                case("TRUE AND FALSE",          "false"),
                case("`true`",                  "true"),
                // numbers
                case("5",                       "true"),
                case("`0e0`",                   "false"),
                case("1.1",                     "true"),
                // timestamp
                case("`2007-10-10T`",           EVALUATOR_INVALID_CAST),
                // text
                case("'hello'",                 "false"),
                case("'TrUe'",                  "true"),
                case("""`"FALSE"`""",           "false"),
                case("""`'true'`""",            "true"),
                // lob
                case("""`{{""}}`""",            EVALUATOR_INVALID_CAST),
                case("""`{{"goodbye"}}`""",     EVALUATOR_INVALID_CAST),
                case("""`{{"false"}}`""",       EVALUATOR_INVALID_CAST),
                case("""`{{"true"}}`""",        EVALUATOR_INVALID_CAST),
                case("`{{}}`",                  EVALUATOR_INVALID_CAST),
                case("`{{Z29vZGJ5ZQ==}}`",      EVALUATOR_INVALID_CAST), // goodbye
                case("`{{ZmFsc2U=}}`",          EVALUATOR_INVALID_CAST), // false
                case("`{{dHJ1ZQ==}}`",          EVALUATOR_INVALID_CAST), // true
                // list
                case("`[]`",                    EVALUATOR_INVALID_CAST),
                case("`[true]`",                EVALUATOR_INVALID_CAST),
                case("`[false]`",               EVALUATOR_INVALID_CAST),
                case("`[true, false]`",         EVALUATOR_INVALID_CAST),
                // sexp
                case("`()`",                    EVALUATOR_INVALID_CAST),
                case("`(true)`",                EVALUATOR_INVALID_CAST),
                case("`(false)`",               EVALUATOR_INVALID_CAST),
                // struct
                case("`{}`",                    EVALUATOR_INVALID_CAST),
                case("{}",                      EVALUATOR_INVALID_CAST),
                case("`{a:true}`",              EVALUATOR_INVALID_CAST),
                case("{'b':true}",              EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>",                    EVALUATOR_INVALID_CAST),
                case("<<true>>",                EVALUATOR_INVALID_CAST),
                case("<<false>>",               EVALUATOR_INVALID_CAST)
            ).types(BOOL.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          "0"),
                case("`true`",                  "1"),
                // numbers
                case("5",                       "5"),
                case("`0e0`",                   "0"),
                case("1.1",                     "1"),
                case("1.9",                     "1"),
                case("-20.1",                   "-20"),
                case("-20.9",                   "-20"),
                // timestamp
                case("`2007-10-10T`",           EVALUATOR_INVALID_CAST),
                // text
                case("'hello'",                 EVALUATOR_CAST_FAILED),
                case("'-20'",                   "-20"),
                case("""`"1000"`""",            "1000"),
                case("""`'2e100'`""",           EVALUATOR_CAST_FAILED),
                case("""`'2d100'`""",           EVALUATOR_CAST_FAILED),
                // lob
                case("""`{{""}}`""",            EVALUATOR_INVALID_CAST),
                case("""`{{"0"}}`""",           EVALUATOR_INVALID_CAST),
                case("""`{{"1.0"}}`""",         EVALUATOR_INVALID_CAST),
                case("""`{{"2e10"}}`""",        EVALUATOR_INVALID_CAST),
                case("`{{}}`",                  EVALUATOR_INVALID_CAST),
                case("`{{MA==}}`",              EVALUATOR_INVALID_CAST), // 0
                case("`{{MS4w}}`",              EVALUATOR_INVALID_CAST), // 1.0
                case("`{{MmUxMA==}}`",          EVALUATOR_INVALID_CAST), // 2e10
                // list
                case("`[]`",                    EVALUATOR_INVALID_CAST),
                case("`[1]`",                   EVALUATOR_INVALID_CAST),
                case("`[-2, 0]`",               EVALUATOR_INVALID_CAST),
                // sexp
                case("`()`",                    EVALUATOR_INVALID_CAST),
                case("`(1)`",                   EVALUATOR_INVALID_CAST),
                case("`(0)`",                   EVALUATOR_INVALID_CAST),
                // struct
                case("`{}`",                    EVALUATOR_INVALID_CAST),
                case("{}",                      EVALUATOR_INVALID_CAST),
                case("`{a:12}`",                EVALUATOR_INVALID_CAST),
                case("{'b':-4}",                EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>",                    EVALUATOR_INVALID_CAST),
                case("<<14>>",                  EVALUATOR_INVALID_CAST),
                case("<<20>>",                  EVALUATOR_INVALID_CAST)
            ).types(INT.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          "0e0"),
                case("`true`",                  "1e0"),
                // numbers
                case("5",                       "5e0"),
                case("`0e0`",                   "0e0"),
                case("1.1",                     "1.1e0"),
                case("-20.1",                   "-20.1e0"),
                // timestamp
                case("`2007-10-10T`",           EVALUATOR_INVALID_CAST),
                // text
                case("'hello'",                 EVALUATOR_CAST_FAILED),
                case("'-20'",                   "-20e0"),
                case("""`"1000"`""",            "1000e0"),
                case("""`'2e100'`""",           "2e100"),
                case("""`'2d100'`""",           EVALUATOR_CAST_FAILED),
                // lob
                case("""`{{""}}`""",            EVALUATOR_INVALID_CAST),
                case("""`{{"0"}}`""",           EVALUATOR_INVALID_CAST),
                case("""`{{"1.0"}}`""",         EVALUATOR_INVALID_CAST),
                case("""`{{"2e10"}}`""",        EVALUATOR_INVALID_CAST),
                case("`{{}}`",                  EVALUATOR_INVALID_CAST),
                case("`{{MA==}}`",              EVALUATOR_INVALID_CAST), // 0
                case("`{{MS4w}}`",              EVALUATOR_INVALID_CAST), // 1.0
                case("`{{MmUxMA==}}`",          EVALUATOR_INVALID_CAST), // 2e10
                // list
                case("`[]`",                    EVALUATOR_INVALID_CAST),
                case("`[1e0]`",                 EVALUATOR_INVALID_CAST),
                case("`[-2e0, 0e0]`",           EVALUATOR_INVALID_CAST),
                // sexp
                case("`()`",                    EVALUATOR_INVALID_CAST),
                case("`(1e0)`",                 EVALUATOR_INVALID_CAST),
                case("`(0e0)`",                 EVALUATOR_INVALID_CAST),
                // struct
                case("`{}`",                    EVALUATOR_INVALID_CAST),
                case("{}",                      EVALUATOR_INVALID_CAST),
                case("`{a:12e0}`",              EVALUATOR_INVALID_CAST),
                case("{'b':`-4e0`}",            EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>",                    EVALUATOR_INVALID_CAST),
                case("<<`14e0`>>",              EVALUATOR_INVALID_CAST),
                case("<<`20e0`>>",              EVALUATOR_INVALID_CAST)
            ).types(FLOAT.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          "0d0"),
                case("`true`",                  "1d0"),
                // numbers
                case("5",                       "5d0"),
                case("`0e0`",                   "0."),  // TODO formalize this behavior
                case("`1e0`",                   "1."),  // TODO formalize this behavior
                case("1.1",                     "1.1d0"),
                case("-20.1",                   "-20.1d0"),
                // timestamp
                case("`2007-10-10T`",           EVALUATOR_INVALID_CAST),
                // text
                case("'hello'",                 EVALUATOR_CAST_FAILED),
                case("'-20'",                   "-20d0"),
                case("""`"1000"`""",            "1000d0"),
                case("""`'2e100'`""",           "2d100"),
                case("""`'2d100'`""",           EVALUATOR_CAST_FAILED),
                // lob
                case("""`{{""}}`""",            EVALUATOR_INVALID_CAST),
                case("""`{{"0"}}`""",           EVALUATOR_INVALID_CAST),
                case("""`{{"1.0"}}`""",         EVALUATOR_INVALID_CAST),
                case("""`{{"2e10"}}`""",        EVALUATOR_INVALID_CAST),
                case("`{{}}`",                  EVALUATOR_INVALID_CAST),
                case("`{{MA==}}`",              EVALUATOR_INVALID_CAST), // 0
                case("`{{MS4w}}`",              EVALUATOR_INVALID_CAST), // 1.0
                case("`{{MmUxMA==}}`",          EVALUATOR_INVALID_CAST), // 2e10
                // list
                case("`[]`",                    EVALUATOR_INVALID_CAST),
                case("`[1d0]`",                 EVALUATOR_INVALID_CAST),
                case("`[-2d0, 0d0]`",           EVALUATOR_INVALID_CAST),
                // sexp
                case("`()`",                    EVALUATOR_INVALID_CAST),
                case("`(1d0)`",                 EVALUATOR_INVALID_CAST),
                case("`(0d0)`",                 EVALUATOR_INVALID_CAST),
                // struct
                case("`{}`",                    EVALUATOR_INVALID_CAST),
                case("{}",                      EVALUATOR_INVALID_CAST),
                case("`{a:12d0}`",              EVALUATOR_INVALID_CAST),
                case("{'b':`-4d0`}",            EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>",                    EVALUATOR_INVALID_CAST),
                case("<<`14d0`>>",              EVALUATOR_INVALID_CAST),
                case("<<`20d0`>>",              EVALUATOR_INVALID_CAST)
            ).types(DECIMAL.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          EVALUATOR_INVALID_CAST),
                case("`true`",                  EVALUATOR_INVALID_CAST),
                // numbers
                case("5",                       EVALUATOR_INVALID_CAST),
                case("`0e0`",                   EVALUATOR_INVALID_CAST),
                case("1.1",                     EVALUATOR_INVALID_CAST),
                case("-20.1",                   EVALUATOR_INVALID_CAST),
                // timestamp
                case("`2007-10-10T`",           "2007-10-10T"),
                // text
                case("'hello'",                 EVALUATOR_CAST_FAILED),
                case("'2016-03-01T01:12:12Z'",  "2016-03-01T01:12:12Z"),
                case("""`"2001-01-01"`""",      "2001-01-01T"),
                case("""`'2000T'`""",           "2000T"),
                case("""`'1999-04T'`""",        "1999-04T"),
                // lob
                case("""`{{""}}`""",            EVALUATOR_INVALID_CAST),
                case("`{{}}`",                  EVALUATOR_INVALID_CAST),
                // list
                case("`[]`",                    EVALUATOR_INVALID_CAST),
                // sexp
                case("`()`",                    EVALUATOR_INVALID_CAST),
                // struct
                case("`{}`",                    EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>",                    EVALUATOR_INVALID_CAST)
            ).types(TIMESTAMP.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          "'false'"),
                case("`true`",                  "'true'"),
                // numbers
                case("5",                       "'5'"),
                case("`0e0`",                   "'0.0'"),
                case("1.1",                     "'1.1'"),
                case("-20.1",                   "'-20.1'"),
                // timestamp
                case("`2007-10-10T`",           "'2007-10-10'"),
                // text
                case("'hello'",                 "'hello'"),
                case("'-20'",                   "'-20'"),
                case("""`"1000"`""",            "'1000'"),
                case("""`'2e100'`""",           "'2e100'"),
                case("""`'2d100'`""",           "'2d100'"),
                // lob
                case("""`{{""}}`""",            EVALUATOR_INVALID_CAST),
                case("""`{{"0"}}`""",           EVALUATOR_INVALID_CAST),
                case("""`{{"1.0"}}`""",         EVALUATOR_INVALID_CAST),
                case("""`{{"2e10"}}`""",        EVALUATOR_INVALID_CAST),
                case("`{{}}`",                  EVALUATOR_INVALID_CAST),
                case("`{{MA==}}`",              EVALUATOR_INVALID_CAST),  // 0
                case("`{{MS4w}}`",              EVALUATOR_INVALID_CAST),  // 1.0
                case("`{{MmUxMA==}}`",          EVALUATOR_INVALID_CAST),  // 2e10
                // list
                case("`[]`",                    EVALUATOR_INVALID_CAST),
                case("['hello']",               EVALUATOR_INVALID_CAST),
                case("`[-2d0, 0d0]`",           EVALUATOR_INVALID_CAST),
                // sexp
                case("`()`",                    EVALUATOR_INVALID_CAST),
                case("`(1d0)`",                 EVALUATOR_INVALID_CAST),
                case("`(0d0)`",                 EVALUATOR_INVALID_CAST),
                // struct
                case("`{}`",                    EVALUATOR_INVALID_CAST),
                case("{}",                      EVALUATOR_INVALID_CAST),
                case("`{a:12d0}`",              EVALUATOR_INVALID_CAST),
                case("{'b':`-4d0`}",            EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>",                    EVALUATOR_INVALID_CAST),
                case("<<`14d0`>>",              EVALUATOR_INVALID_CAST),
                case("<<`20d0`>>",              EVALUATOR_INVALID_CAST)
            ).types(SYMBOL.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          "\"false\""),
                case("`true`",                  "\"true\""),
                // numbers
                case("5",                       "\"5\""),
                case("`0e0`",                   "\"0.0\""),
                case("1.1",                     "\"1.1\""),
                case("-20.1",                   "\"-20.1\""),
                // timestamp
                case("`2007-10-10T`",           "\"2007-10-10\""),
                // text
                case("'hello'",                 "\"hello\""),
                case("'-20'",                   "\"-20\""),
                case("""`"1000"`""",            "\"1000\""),
                case("""`'2e100'`""",           "\"2e100\""),
                case("""`'2d100'`""",           "\"2d100\""),
                // lob
                case("""`{{""}}`""",            EVALUATOR_INVALID_CAST),
                case("""`{{"0"}}`""",           EVALUATOR_INVALID_CAST),
                case("""`{{"1.0"}}`""",         EVALUATOR_INVALID_CAST),
                case("""`{{"2e10"}}`""",        EVALUATOR_INVALID_CAST),
                case("`{{}}`",                  EVALUATOR_INVALID_CAST),
                case("`{{MA==}}`",              EVALUATOR_INVALID_CAST),  // 0
                case("`{{MS4w}}`",              EVALUATOR_INVALID_CAST),  // 1.0
                case("`{{MmUxMA==}}`",          EVALUATOR_INVALID_CAST),  // 2e10
                // list
                case("`[]`",                    EVALUATOR_INVALID_CAST),
                case("['hello']",               EVALUATOR_INVALID_CAST),
                case("`[-2d0, 0d0]`",           EVALUATOR_INVALID_CAST),
                // sexp
                case("`()`",                    EVALUATOR_INVALID_CAST),
                case("`(1d0)`",                 EVALUATOR_INVALID_CAST),
                case("`(0d0)`",                 EVALUATOR_INVALID_CAST),
                // struct
                case("`{}`",                    EVALUATOR_INVALID_CAST),
                case("{}",                      EVALUATOR_INVALID_CAST),
                case("`{a:12d0}`",              EVALUATOR_INVALID_CAST),
                case("{'b':`-4d0`}",            EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>",                    EVALUATOR_INVALID_CAST),
                case("<<`14d0`>>",              EVALUATOR_INVALID_CAST),
                case("<<'a', <<'hello'>>>>",    EVALUATOR_INVALID_CAST),
                case("<<`20d0`>>",              EVALUATOR_INVALID_CAST)
            ).types(STRING.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          EVALUATOR_INVALID_CAST),
                case("`true`",                  EVALUATOR_INVALID_CAST),
                // numbers
                case("5",                       EVALUATOR_INVALID_CAST),
                case("`0e0`",                   EVALUATOR_INVALID_CAST),
                case("1.1",                     EVALUATOR_INVALID_CAST),
                case("-20.1",                   EVALUATOR_INVALID_CAST),
                // timestamp
                case("`2007-10-10T`",           EVALUATOR_INVALID_CAST),
                // text
                case("'hello'",                 EVALUATOR_INVALID_CAST),
                case("'-20'",                   EVALUATOR_INVALID_CAST),
                case("""`"1000"`""",            EVALUATOR_INVALID_CAST),
                case("""`'2e100'`""",           EVALUATOR_INVALID_CAST),
                case("""`'2d100'`""",           EVALUATOR_INVALID_CAST),
                // lob
                case("""`{{""}}`""",            """{{""}}"""),
                case("""`{{"0"}}`""",           """{{"0"}}"""),
                case("""`{{"1.0"}}`""",         """{{"1.0"}}"""),
                case("""`{{"2e10"}}`""",        """{{"2e10"}}"""),
                case("`{{}}`",                  """{{""}}"""),
                case("`{{MA==}}`",              """{{"0"}}"""),
                case("`{{MS4w}}`",              """{{"1.0"}}"""),
                case("`{{MmUxMA==}}`",          """{{"2e10"}}"""),
                // list
                case("`[]`",                    EVALUATOR_INVALID_CAST),
                case("['hello']",               EVALUATOR_INVALID_CAST),
                case("`[-2d0, 0d0]`",           EVALUATOR_INVALID_CAST),
                // sexp
                case("`()`",                    EVALUATOR_INVALID_CAST),
                case("`(1d0)`",                 EVALUATOR_INVALID_CAST),
                case("`(0d0)`",                 EVALUATOR_INVALID_CAST),
                // struct
                case("`{}`",                    EVALUATOR_INVALID_CAST),
                case("{}",                      EVALUATOR_INVALID_CAST),
                case("`{a:12d0}`",              EVALUATOR_INVALID_CAST),
                case("{'b':`-4d0`}",            EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>",                    EVALUATOR_INVALID_CAST),
                case("<<`14d0`>>",              EVALUATOR_INVALID_CAST),
                case("<<`20d0`>>",              EVALUATOR_INVALID_CAST)
            ).types(CLOB.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          EVALUATOR_INVALID_CAST),
                case("`true`",                  EVALUATOR_INVALID_CAST),
                // numbers
                case("5",                       EVALUATOR_INVALID_CAST),
                case("`0e0`",                   EVALUATOR_INVALID_CAST),
                case("1.1",                     EVALUATOR_INVALID_CAST),
                case("-20.1",                   EVALUATOR_INVALID_CAST),
                // timestamp
                case("`2007-10-10T`",           EVALUATOR_INVALID_CAST),
                // text
                case("'hello'",                 EVALUATOR_INVALID_CAST),
                case("'-20'",                   EVALUATOR_INVALID_CAST),
                case("""`"1000"`""",            EVALUATOR_INVALID_CAST),
                case("""`'2e100'`""",           EVALUATOR_INVALID_CAST),
                case("""`'2d100'`""",           EVALUATOR_INVALID_CAST),
                // lob
                case("""`{{""}}`""",            """{{}}"""),
                case("""`{{"0"}}`""",           """{{MA==}}"""),
                case("""`{{"1.0"}}`""",         """{{MS4w}}"""),
                case("""`{{"2e10"}}`""",        """{{MmUxMA==}}"""),
                case("`{{}}`",                  """{{}}"""),
                case("`{{MA==}}`",              """{{MA==}}"""),     // 0
                case("`{{MS4w}}`",              """{{MS4w}}"""),     // 1.0
                case("`{{MmUxMA==}}`",          """{{MmUxMA==}}"""), // 2e10
                // list
                case("`[]`",                    EVALUATOR_INVALID_CAST),
                case("['hello']",               EVALUATOR_INVALID_CAST),
                case("`[-2d0, 0d0]`",           EVALUATOR_INVALID_CAST),
                // sexp
                case("`()`",                    EVALUATOR_INVALID_CAST),
                case("`(1d0)`",                 EVALUATOR_INVALID_CAST),
                case("`(0d0)`",                 EVALUATOR_INVALID_CAST),
                // struct
                case("`{}`",                    EVALUATOR_INVALID_CAST),
                case("{}",                      EVALUATOR_INVALID_CAST),
                case("`{a:12d0}`",              EVALUATOR_INVALID_CAST),
                case("{'b':`-4d0`}",            EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>",                    EVALUATOR_INVALID_CAST),
                case("<<`14d0`>>",              EVALUATOR_INVALID_CAST),
                case("<<`20d0`>>",              EVALUATOR_INVALID_CAST)
            ).types(BLOB.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          EVALUATOR_INVALID_CAST),
                case("`true`",                  EVALUATOR_INVALID_CAST),
                // numbers
                case("5",                       EVALUATOR_INVALID_CAST),
                case("`0e0`",                   EVALUATOR_INVALID_CAST),
                case("1.1",                     EVALUATOR_INVALID_CAST),
                case("-20.1",                   EVALUATOR_INVALID_CAST),
                // timestamp
                case("`2007-10-10T`",           EVALUATOR_INVALID_CAST),
                // text
                case("'hello'",                 EVALUATOR_INVALID_CAST),
                case("'-20'",                   EVALUATOR_INVALID_CAST),
                case("""`"1000"`""",            EVALUATOR_INVALID_CAST),
                case("""`'2e100'`""",           EVALUATOR_INVALID_CAST),
                case("""`'2d100'`""",           EVALUATOR_INVALID_CAST),
                // lob
                case("""`{{""}}`""",            EVALUATOR_INVALID_CAST),
                case("""`{{"0"}}`""",           EVALUATOR_INVALID_CAST),
                case("""`{{"1.0"}}`""",         EVALUATOR_INVALID_CAST),
                case("""`{{"2e10"}}`""",        EVALUATOR_INVALID_CAST),
                case("`{{}}`",                  EVALUATOR_INVALID_CAST),
                case("`{{MA==}}`",              EVALUATOR_INVALID_CAST), // 0
                case("`{{MS4w}}`",              EVALUATOR_INVALID_CAST), // 1.0
                case("`{{MmUxMA==}}`",          EVALUATOR_INVALID_CAST), // 2e10
                // list
                case("`[]`",                    "[]"),
                case("['hello']",               "[\"hello\"]"),
                case("`[-2d0, 0d0]`",           "[-2d0, 0d0]"),
                // sexp
                case("`()`",                    "[]"),
                case("`(1d0)`",                 "[1d0]"),
                case("`(0d0)`",                 "[0d0]"),
                // struct
                case("`{}`",                    EVALUATOR_INVALID_CAST),
                case("{}",                      EVALUATOR_INVALID_CAST),
                case("`{a:12d0}`",              EVALUATOR_INVALID_CAST),
                case("{'b':`-4d0`}",            EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>",                    "[]"),      // TODO bag verification
                case("<<`14d0`>>",              "[14d0]"),  // TODO bag verification
                case("<<`20d0`>>",              "[20d0]")   // TODO bag verification
            ).types(LIST.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          EVALUATOR_INVALID_CAST),
                case("`true`",                  EVALUATOR_INVALID_CAST),
                // numbers
                case("5",                       EVALUATOR_INVALID_CAST),
                case("`0e0`",                   EVALUATOR_INVALID_CAST),
                case("1.1",                     EVALUATOR_INVALID_CAST),
                case("-20.1",                   EVALUATOR_INVALID_CAST),
                // timestamp
                case("`2007-10-10T`",           EVALUATOR_INVALID_CAST),
                // text
                case("'hello'",                 EVALUATOR_INVALID_CAST),
                case("'-20'",                   EVALUATOR_INVALID_CAST),
                case("""`"1000"`""",            EVALUATOR_INVALID_CAST),
                case("""`'2e100'`""",           EVALUATOR_INVALID_CAST),
                case("""`'2d100'`""",           EVALUATOR_INVALID_CAST),
                // lob
                case("""`{{""}}`""",            EVALUATOR_INVALID_CAST),
                case("""`{{"0"}}`""",           EVALUATOR_INVALID_CAST),
                case("""`{{"1.0"}}`""",         EVALUATOR_INVALID_CAST),
                case("""`{{"2e10"}}`""",        EVALUATOR_INVALID_CAST),
                case("`{{}}`",                  EVALUATOR_INVALID_CAST),
                case("`{{MA==}}`",              EVALUATOR_INVALID_CAST), // 0
                case("`{{MS4w}}`",              EVALUATOR_INVALID_CAST), // 1.0
                case("`{{MmUxMA==}}`",          EVALUATOR_INVALID_CAST), // 2e10
                // list
                case("`[]`",                    "()"),
                case("['hello']",               "(\"hello\")"),
                case("`[-2d0, 0d0]`",           "(-2d0 0d0)"),
                // sexp
                case("`()`",                    "()"),
                case("`(1d0)`",                 "(1d0)"),
                case("`(0d0)`",                 "(0d0)"),
                // struct
                case("`{}`",                    EVALUATOR_INVALID_CAST),
                case("{}",                      EVALUATOR_INVALID_CAST),
                case("`{a:12d0}`",              EVALUATOR_INVALID_CAST),
                case("{'b':`-4d0`}",            EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>",                    "()"),
                case("<<`14d0`>>",              "(14d0)"),
                case("<<`20d0`>>",              "(20d0)")
            ).types(SEXP.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          EVALUATOR_INVALID_CAST),
                case("`true`",                  EVALUATOR_INVALID_CAST),
                // numbers
                case("5",                       EVALUATOR_INVALID_CAST),
                case("`0e0`",                   EVALUATOR_INVALID_CAST),
                case("1.1",                     EVALUATOR_INVALID_CAST),
                case("-20.1",                   EVALUATOR_INVALID_CAST),
                // timestamp
                case("`2007-10-10T`",           EVALUATOR_INVALID_CAST),
                // text
                case("'hello'",                 EVALUATOR_INVALID_CAST),
                case("'-20'",                   EVALUATOR_INVALID_CAST),
                case("""`"1000"`""",            EVALUATOR_INVALID_CAST),
                case("""`'2e100'`""",           EVALUATOR_INVALID_CAST),
                case("""`'2d100'`""",           EVALUATOR_INVALID_CAST),
                // lob
                case("""`{{""}}`""",            EVALUATOR_INVALID_CAST),
                case("""`{{"0"}}`""",           EVALUATOR_INVALID_CAST),
                case("""`{{"1.0"}}`""",         EVALUATOR_INVALID_CAST),
                case("""`{{"2e10"}}`""",        EVALUATOR_INVALID_CAST),
                case("`{{}}`",                  EVALUATOR_INVALID_CAST),
                case("`{{MA==}}`",              EVALUATOR_INVALID_CAST), // 0
                case("`{{MS4w}}`",              EVALUATOR_INVALID_CAST), // 1.0
                case("`{{MmUxMA==}}`",          EVALUATOR_INVALID_CAST), // 2e10
                // list
                case("`[]`",                    EVALUATOR_INVALID_CAST),
                case("['hello']",               EVALUATOR_INVALID_CAST),
                case("`[-2d0, 0d0]`",           EVALUATOR_INVALID_CAST),
                // sexp
                case("`()`",                    EVALUATOR_INVALID_CAST),
                case("`(1d0)`",                 EVALUATOR_INVALID_CAST),
                case("`(0d0)`",                 EVALUATOR_INVALID_CAST),
                // struct
                case("`{}`",                    "{}"),
                case("{}",                      "{}"),
                case("`{a:12d0}`",              "{a:12d0}"),
                case("{'b':`-4d0`}",            "{b:-4d0}"),
                // bag
                case("<<>>",                    EVALUATOR_INVALID_CAST),
                case("<<`14d0`>>",              EVALUATOR_INVALID_CAST),
                case("<<`20d0`>>",              EVALUATOR_INVALID_CAST)
            ).types(STRUCT.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          EVALUATOR_INVALID_CAST),
                case("`true`",                  EVALUATOR_INVALID_CAST),
                // numbers
                case("5",                       EVALUATOR_INVALID_CAST),
                case("`0e0`",                   EVALUATOR_INVALID_CAST),
                case("1.1",                     EVALUATOR_INVALID_CAST),
                case("-20.1",                   EVALUATOR_INVALID_CAST),
                // timestamp
                case("`2007-10-10T`",           EVALUATOR_INVALID_CAST),
                // text
                case("'hello'",                 EVALUATOR_INVALID_CAST),
                case("'-20'",                   EVALUATOR_INVALID_CAST),
                case("""`"1000"`""",            EVALUATOR_INVALID_CAST),
                case("""`'2e100'`""",           EVALUATOR_INVALID_CAST),
                case("""`'2d100'`""",           EVALUATOR_INVALID_CAST),
                // lob
                case("""`{{""}}`""",            EVALUATOR_INVALID_CAST),
                case("""`{{"0"}}`""",           EVALUATOR_INVALID_CAST),
                case("""`{{"1.0"}}`""",         EVALUATOR_INVALID_CAST),
                case("""`{{"2e10"}}`""",        EVALUATOR_INVALID_CAST),
                case("`{{}}`",                  EVALUATOR_INVALID_CAST),
                case("`{{MA==}}`",              EVALUATOR_INVALID_CAST), // 0
                case("`{{MS4w}}`",              EVALUATOR_INVALID_CAST), // 1.0
                case("`{{MmUxMA==}}`",          EVALUATOR_INVALID_CAST), // 2e10
                // list
                case("`[]`",                    "[]"),          // TODO bag verification
                case("['hello']",               "[\"hello\"]"), // TODO bag verification
                case("`[-2d0, 0d0]`",           "[-2d0, 0d0]"), // TODO bag verification
                // sexp
                case("`()`",                    "[]"),          // TODO bag verification
                case("`(1d0)`",                 "[1d0]"),       // TODO bag verification
                case("`(0d0)`",                 "[0d0]"),       // TODO bag verification
                // struct
                case("`{}`",                    EVALUATOR_INVALID_CAST),
                case("{}",                      EVALUATOR_INVALID_CAST),
                case("`{a:12d0}`",              EVALUATOR_INVALID_CAST),
                case("{'b':`-4d0`}",            EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>",                    "[]"),          // TODO bag verification
                case("<<`14d0`>>",              "[14d0]"),      // TODO bag verification
                case("<<`20d0`>>",              "[20d0]")       // TODO bag verification
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
                if(castCase.expectedErrorCode == null) {
                    fail("CastCase $castCase did not have an expected value or expected error code.")
                }
                assertEquals(castCase.expectedErrorCode, e.errorCode)
            }
        }
        else -> assertEval(castCase.expression, castCase.expected)
    }
}

