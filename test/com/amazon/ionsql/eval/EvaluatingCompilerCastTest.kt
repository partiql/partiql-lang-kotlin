/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.eval.ExprValueType.*
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
     */
    data class CastCase(val source: String, val type: String, val expected: String?) {
        val expression = "CAST($source AS $type)"
        override fun toString(): String = expression
    }

    /** Partial application of the source expression and the expected Ion value without type. */
    fun case(source: String, expected: String?): (String) -> CastCase = {
        CastCase(source, it, expected)
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
                case("`2007-10-10T`",           null),
                // text
                case("'hello'",                 "false"),
                case("'TrUe'",                  "true"),
                case("""`"FALSE"`""",           "false"),
                case("""`'true'`""",            "true"),
                // lob
                case("""`{{""}}`""",            null),
                case("""`{{"goodbye"}}`""",     null),
                case("""`{{"false"}}`""",       null),
                case("""`{{"true"}}`""",        null),
                case("`{{}}`",                  null),
                case("`{{Z29vZGJ5ZQ==}}`",      null), // goodbye
                case("`{{ZmFsc2U=}}`",          null), // false
                case("`{{dHJ1ZQ==}}`",          null), // true
                // list
                case("`[]`",                    null),
                case("`[true]`",                null),
                case("`[false]`",               null),
                case("`[true, false]`",         null),
                // sexp
                case("`()`",                    null),
                case("`(true)`",                null),
                case("`(false)`",               null),
                // struct
                case("`{}`",                    null),
                case("{}",                      null),
                case("`{a:true}`",              null),
                case("{'b':true}",              null),
                // bag
                case("<<>>",                    null),
                case("<<true>>",                null),
                case("<<false>>",               null)
            ).types(BOOL.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          "0"),
                case("`true`",                  "1"),
                // numbers
                case("5",                       "5"),
                case("`0e0`",                   "0"),
                case("1.1",                     "1"),
                case("-20.1",                   "-20"),
                // timestamp
                case("`2007-10-10T`",           null),
                // text
                case("'hello'",                 null),
                case("'-20'",                   "-20"),
                case("""`"1000"`""",            "1000"),
                case("""`'2e100'`""",           null),
                case("""`'2d100'`""",           null),
                // lob
                case("""`{{""}}`""",            null),
                case("""`{{"0"}}`""",           null),
                case("""`{{"1.0"}}`""",         null),
                case("""`{{"2e10"}}`""",        null),
                case("`{{}}`",                  null),
                case("`{{MA==}}`",              null), // 0
                case("`{{MS4w}}`",              null), // 1.0
                case("`{{MmUxMA==}}`",          null), // 2e10
                // list
                case("`[]`",                    null),
                case("`[1]`",                   null),
                case("`[-2, 0]`",               null),
                // sexp
                case("`()`",                    null),
                case("`(1)`",                   null),
                case("`(0)`",                   null),
                // struct
                case("`{}`",                    null),
                case("{}",                      null),
                case("`{a:12}`",                null),
                case("{'b':-4}",                null),
                // bag
                case("<<>>",                    null),
                case("<<14>>",                  null),
                case("<<20>>",                  null)
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
                case("`2007-10-10T`",           null),
                // text
                case("'hello'",                 null),
                case("'-20'",                   "-20e0"),
                case("""`"1000"`""",            "1000e0"),
                case("""`'2e100'`""",           "2e100"),
                case("""`'2d100'`""",           null),
                // lob
                case("""`{{""}}`""",            null),
                case("""`{{"0"}}`""",           null),
                case("""`{{"1.0"}}`""",         null),
                case("""`{{"2e10"}}`""",        null),
                case("`{{}}`",                  null),
                case("`{{MA==}}`",              null), // 0
                case("`{{MS4w}}`",              null), // 1.0
                case("`{{MmUxMA==}}`",          null), // 2e10
                // list
                case("`[]`",                    null),
                case("`[1e0]`",                 null),
                case("`[-2e0, 0e0]`",           null),
                // sexp
                case("`()`",                    null),
                case("`(1e0)`",                 null),
                case("`(0e0)`",                 null),
                // struct
                case("`{}`",                    null),
                case("{}",                      null),
                case("`{a:12e0}`",              null),
                case("{'b':`-4e0`}",            null),
                // bag
                case("<<>>",                    null),
                case("<<`14e0`>>",              null),
                case("<<`20e0`>>",              null)
            ).types(FLOAT.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          "0d0"),
                case("`true`",                  "1d0"),
                // numbers
                case("5",                       "5d0"),
                case("`0e0`",                   "0d-1"), // TODO formalize this behavior
                case("`1e0`",                   "1.0"),  // TODO formalize this behavior
                case("1.1",                     "1.1d0"),
                case("-20.1",                   "-20.1d0"),
                // timestamp
                case("`2007-10-10T`",           null),
                // text
                case("'hello'",                 null),
                case("'-20'",                   "-20d0"),
                case("""`"1000"`""",            "1000d0"),
                case("""`'2e100'`""",           "2d100"),
                case("""`'2d100'`""",           null),
                // lob
                case("""`{{""}}`""",            null),
                case("""`{{"0"}}`""",           null),
                case("""`{{"1.0"}}`""",         null),
                case("""`{{"2e10"}}`""",        null),
                case("`{{}}`",                  null),
                case("`{{MA==}}`",              null), // 0
                case("`{{MS4w}}`",              null), // 1.0
                case("`{{MmUxMA==}}`",          null), // 2e10
                // list
                case("`[]`",                    null),
                case("`[1d0]`",                 null),
                case("`[-2d0, 0d0]`",           null),
                // sexp
                case("`()`",                    null),
                case("`(1d0)`",                 null),
                case("`(0d0)`",                 null),
                // struct
                case("`{}`",                    null),
                case("{}",                      null),
                case("`{a:12d0}`",              null),
                case("{'b':`-4d0`}",            null),
                // bag
                case("<<>>",                    null),
                case("<<`14d0`>>",              null),
                case("<<`20d0`>>",              null)
            ).types(DECIMAL.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          null),
                case("`true`",                  null),
                // numbers
                case("5",                       null),
                case("`0e0`",                   null),
                case("1.1",                     null),
                case("-20.1",                   null),
                // timestamp
                case("`2007-10-10T`",           "2007-10-10T"),
                // text
                case("'hello'",                 null),
                case("'2016-03-01T01:12:12Z'",  "2016-03-01T01:12:12Z"),
                case("""`"2001-01-01"`""",      "2001-01-01T"),
                case("""`'2000T'`""",           "2000T"),
                case("""`'1999-04T'`""",        "1999-04T"),
                // lob
                case("""`{{""}}`""",            null),
                case("`{{}}`",                  null),
                // list
                case("`[]`",                    null),
                // sexp
                case("`()`",                    null),
                // struct
                case("`{}`",                    null),
                // bag
                case("<<>>",                    null)
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
                case("""`{{""}}`""",            """'{{""}}'"""),
                case("""`{{"0"}}`""",           """'{{"0"}}'"""),
                case("""`{{"1.0"}}`""",         """'{{"1.0"}}'"""),
                case("""`{{"2e10"}}`""",        """'{{"2e10"}}'"""),
                case("`{{}}`",                  """'{{}}'"""),
                case("`{{MA==}}`",              """'{{MA==}}'"""),      // 0
                case("`{{MS4w}}`",              """'{{MS4w}}'"""),      // 1.0
                case("`{{MmUxMA==}}`",          """'{{MmUxMA==}}'"""),  // 2e10
                // list
                case("`[]`",                    """'[]'"""),
                case("['hello']",               """'["hello"]'"""),
                case("`[-2d0, 0d0]`",           """'[-2.,0.]'"""),
                // sexp
                case("`()`",                    """'()'"""),
                case("`(1d0)`",                 """'(1.)'"""),
                case("`(0d0)`",                 """'(0.)'"""),
                // struct
                case("`{}`",                    """'{}'"""),
                case("{}",                      """'{}'"""),
                case("`{a:12d0}`",              """'{a:12.}'"""),
                case("{'b':`-4d0`}",            """'{b:-4.}'"""),
                // bag
                case("<<>>",                    """'<<>>'"""),
                case("<<`14d0`>>",              """'<<`14.`>>'"""),
                case("<<`20d0`>>",              """'<<`20.`>>'""")
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
                case("""`{{""}}`""",            """'''{{""}}'''"""),
                case("""`{{"0"}}`""",           """'''{{"0"}}'''"""),
                case("""`{{"1.0"}}`""",         """'''{{"1.0"}}'''"""),
                case("""`{{"2e10"}}`""",        """'''{{"2e10"}}'''"""),
                case("`{{}}`",                  """'''{{}}'''"""),
                case("`{{MA==}}`",              """'''{{MA==}}'''"""),      // 0
                case("`{{MS4w}}`",              """'''{{MS4w}}'''"""),      // 1.0
                case("`{{MmUxMA==}}`",          """'''{{MmUxMA==}}'''"""),  // 2e10
                // list
                case("`[]`",                    """'''[]'''"""),
                case("['hello']",               """'''["hello"]'''"""),
                case("`[-2d0, 0d0]`",           """'''[-2.,0.]'''"""),
                // sexp
                case("`()`",                    """'''()'''"""),
                case("`(1d0)`",                 """'''(1.)'''"""),
                case("`(0d0)`",                 """'''(0.)'''"""),
                // struct
                case("`{}`",                    """'''{}'''"""),
                case("{}",                      """'''{}'''"""),
                case("`{a:12d0}`",              """'''{a:12.}'''"""),
                case("{'b':`-4d0`}",            """'''{b:-4.}'''"""),
                // bag
                case("<<>>",                    """'''<<>>'''"""),
                case("<<`14d0`>>",              """'''<<`14.`>>'''"""),
                case("<<'a', <<'hello'>>>>",    """'''<<`"a"`,<<`"hello"`>>>>'''"""),
                case("<<`20d0`>>",              """'''<<`20.`>>'''""")
            ).types(STRING.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          null),
                case("`true`",                  null),
                // numbers
                case("5",                       null),
                case("`0e0`",                   null),
                case("1.1",                     null),
                case("-20.1",                   null),
                // timestamp
                case("`2007-10-10T`",           null),
                // text
                case("'hello'",                 null),
                case("'-20'",                   null),
                case("""`"1000"`""",            null),
                case("""`'2e100'`""",           null),
                case("""`'2d100'`""",           null),
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
                case("`[]`",                    null),
                case("['hello']",               null),
                case("`[-2d0, 0d0]`",           null),
                // sexp
                case("`()`",                    null),
                case("`(1d0)`",                 null),
                case("`(0d0)`",                 null),
                // struct
                case("`{}`",                    null),
                case("{}",                      null),
                case("`{a:12d0}`",              null),
                case("{'b':`-4d0`}",            null),
                // bag
                case("<<>>",                    null),
                case("<<`14d0`>>",              null),
                case("<<`20d0`>>",              null)
            ).types(CLOB.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          null),
                case("`true`",                  null),
                // numbers
                case("5",                       null),
                case("`0e0`",                   null),
                case("1.1",                     null),
                case("-20.1",                   null),
                // timestamp
                case("`2007-10-10T`",           null),
                // text
                case("'hello'",                 null),
                case("'-20'",                   null),
                case("""`"1000"`""",            null),
                case("""`'2e100'`""",           null),
                case("""`'2d100'`""",           null),
                // lob
                case("""`{{""}}`""",            null),
                case("""`{{"0"}}`""",           null),
                case("""`{{"1.0"}}`""",         null),
                case("""`{{"2e10"}}`""",        null),
                case("`{{}}`",                  null),
                case("`{{MA==}}`",              null), // 0
                case("`{{MS4w}}`",              null), // 1.0
                case("`{{MmUxMA==}}`",          null), // 2e10
                // list
                case("`[]`",                    "[]"),
                case("['hello']",               "[\"hello\"]"),
                case("`[-2d0, 0d0]`",           "[-2d0, 0d0]"),
                // sexp
                case("`()`",                    "[]"),
                case("`(1d0)`",                 "[1d0]"),
                case("`(0d0)`",                 "[0d0]"),
                // struct
                case("`{}`",                    null),
                case("{}",                      null),
                case("`{a:12d0}`",              null),
                case("{'b':`-4d0`}",            null),
                // bag
                case("<<>>",                    "[]"),      // TODO bag verification
                case("<<`14d0`>>",              "[14d0]"),  // TODO bag verification
                case("<<`20d0`>>",              "[20d0]")   // TODO bag verification
            ).types(LIST.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          null),
                case("`true`",                  null),
                // numbers
                case("5",                       null),
                case("`0e0`",                   null),
                case("1.1",                     null),
                case("-20.1",                   null),
                // timestamp
                case("`2007-10-10T`",           null),
                // text
                case("'hello'",                 null),
                case("'-20'",                   null),
                case("""`"1000"`""",            null),
                case("""`'2e100'`""",           null),
                case("""`'2d100'`""",           null),
                // lob
                case("""`{{""}}`""",            null),
                case("""`{{"0"}}`""",           null),
                case("""`{{"1.0"}}`""",         null),
                case("""`{{"2e10"}}`""",        null),
                case("`{{}}`",                  null),
                case("`{{MA==}}`",              null), // 0
                case("`{{MS4w}}`",              null), // 1.0
                case("`{{MmUxMA==}}`",          null), // 2e10
                // list
                case("`[]`",                    "()"),
                case("['hello']",               "(\"hello\")"),
                case("`[-2d0, 0d0]`",           "(-2d0 0d0)"),
                // sexp
                case("`()`",                    "()"),
                case("`(1d0)`",                 "(1d0)"),
                case("`(0d0)`",                 "(0d0)"),
                // struct
                case("`{}`",                    null),
                case("{}",                      null),
                case("`{a:12d0}`",              null),
                case("{'b':`-4d0`}",            null),
                // bag
                case("<<>>",                    "()"),
                case("<<`14d0`>>",              "(14d0)"),
                case("<<`20d0`>>",              "(20d0)")
            ).types(SEXP.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          null),
                case("`true`",                  null),
                // numbers
                case("5",                       null),
                case("`0e0`",                   null),
                case("1.1",                     null),
                case("-20.1",                   null),
                // timestamp
                case("`2007-10-10T`",           null),
                // text
                case("'hello'",                 null),
                case("'-20'",                   null),
                case("""`"1000"`""",            null),
                case("""`'2e100'`""",           null),
                case("""`'2d100'`""",           null),
                // lob
                case("""`{{""}}`""",            null),
                case("""`{{"0"}}`""",           null),
                case("""`{{"1.0"}}`""",         null),
                case("""`{{"2e10"}}`""",        null),
                case("`{{}}`",                  null),
                case("`{{MA==}}`",              null), // 0
                case("`{{MS4w}}`",              null), // 1.0
                case("`{{MmUxMA==}}`",          null), // 2e10
                // list
                case("`[]`",                    null),
                case("['hello']",               null),
                case("`[-2d0, 0d0]`",           null),
                // sexp
                case("`()`",                    null),
                case("`(1d0)`",                 null),
                case("`(0d0)`",                 null),
                // struct
                case("`{}`",                    "{}"),
                case("{}",                      "{}"),
                case("`{a:12d0}`",              "{a:12d0}"),
                case("{'b':`-4d0`}",            "{b:-4d0}"),
                // bag
                case("<<>>",                    null),
                case("<<`14d0`>>",              null),
                case("<<`20d0`>>",              null)
            ).types(STRUCT.sqlTextNames),
            listOf(
                // booleans
                case("TRUE AND FALSE",          null),
                case("`true`",                  null),
                // numbers
                case("5",                       null),
                case("`0e0`",                   null),
                case("1.1",                     null),
                case("-20.1",                   null),
                // timestamp
                case("`2007-10-10T`",           null),
                // text
                case("'hello'",                 null),
                case("'-20'",                   null),
                case("""`"1000"`""",            null),
                case("""`'2e100'`""",           null),
                case("""`'2d100'`""",           null),
                // lob
                case("""`{{""}}`""",            null),
                case("""`{{"0"}}`""",           null),
                case("""`{{"1.0"}}`""",         null),
                case("""`{{"2e10"}}`""",        null),
                case("`{{}}`",                  null),
                case("`{{MA==}}`",              null), // 0
                case("`{{MS4w}}`",              null), // 1.0
                case("`{{MmUxMA==}}`",          null), // 2e10
                // list
                case("`[]`",                    "[]"),          // TODO bag verification
                case("['hello']",               "[\"hello\"]"), // TODO bag verification
                case("`[-2d0, 0d0]`",           "[-2d0, 0d0]"), // TODO bag verification
                // sexp
                case("`()`",                    "[]"),          // TODO bag verification
                case("`(1d0)`",                 "[1d0]"),       // TODO bag verification
                case("`(0d0)`",                 "[0d0]"),       // TODO bag verification
                // struct
                case("`{}`",                    null),
                case("{}",                      null),
                case("`{a:12d0}`",              null),
                case("{'b':`-4d0`}",            null),
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
            } catch (e: EvaluationException) {}
        }
        else -> assertEval(castCase.expression, castCase.expected)
    }
}
