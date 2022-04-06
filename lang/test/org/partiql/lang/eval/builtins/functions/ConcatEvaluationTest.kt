package org.partiql.lang.eval.builtins.functions

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.builtins.ExprFunctionTestCase
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.toIntExact

class ConcatEvaluationTest : EvaluatorTestBase() {
    // Pass test cases
    @ParameterizedTest
    @ArgumentsSource(ConcatPassCases::class)
    fun runPassTests(testCase: ExprFunctionTestCase) = assertEval(testCase.source, testCase.expected)

    class ConcatPassCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // Following are all the valid types:
            // 1. String: 'a'
            // 2. Ion string: `"a"`
            // 3. Ion symbol: `a` or `'a'`
            // 4. null or missing

            // 1st arg: String
            ExprFunctionTestCase("'a' || 'b'", "\"ab\""), // 2nd arg: String
            ExprFunctionTestCase("'a' || `\"b\"`", "\"ab\""), // 2nd arg: Ion String
            ExprFunctionTestCase("'a' || `b`", "\"ab\""), // 2nd arg: Ion symbol ``
            ExprFunctionTestCase("'a' || `'b'`", "\"ab\""), // 2nd arg: Ion symbol `''`
            ExprFunctionTestCase("'a' || null", "null"), // 2nd arg: null
            ExprFunctionTestCase("'a' || missing", "null"), // 2nd arg: missing

            // 1st arg: Ion String
            ExprFunctionTestCase("`\"a\"` || 'b'", "\"ab\""), // 2nd arg: String
            ExprFunctionTestCase("`\"a\"` || `\"b\"`", "\"ab\""), // 2nd arg: Ion String
            ExprFunctionTestCase("`\"a\"` || `b`", "\"ab\""), // 2nd arg: Ion symbol ``
            ExprFunctionTestCase("`\"a\"` || `'b'`", "\"ab\""), // 2nd arg: Ion symbol `''`
            ExprFunctionTestCase("`\"a\"` || null", "null"), // 2nd arg: null
            ExprFunctionTestCase("`\"a\"` || missing", "null"), // 2nd arg: missing

            // 1st arg: Ion symbol (``)
            ExprFunctionTestCase("`a` || 'b'", "\"ab\""), // 2nd arg: String
            ExprFunctionTestCase("`a` || `\"b\"`", "\"ab\""), // 2nd arg: Ion String
            ExprFunctionTestCase("`a` || `b`", "\"ab\""), // 2nd arg: Ion symbol ``
            ExprFunctionTestCase("`a` || `'b'`", "\"ab\""), // 2nd arg: Ion symbol `''`
            ExprFunctionTestCase("`a` || null", "null"), // 2nd arg: null
            ExprFunctionTestCase("`a` || missing", "null"), // 2nd arg: missing

            // 1st arg: Ion symbol (``)
            ExprFunctionTestCase("`'a'` || 'b'", "\"ab\""), // 2nd arg: String
            ExprFunctionTestCase("`'a'` || `\"b\"`", "\"ab\""), // 2nd arg: Ion String
            ExprFunctionTestCase("`'a'` || `b`", "\"ab\""), // 2nd arg: Ion symbol ``
            ExprFunctionTestCase("`'a'` || `'b'`", "\"ab\""), // 2nd arg: Ion symbol `''`
            ExprFunctionTestCase("`'a'` || null", "null"), // 2nd arg: null
            ExprFunctionTestCase("`'a'` || missing", "null"), // 2nd arg: missing

            // 1st arg: null
            ExprFunctionTestCase("null || 'b'", "null"), // 2nd arg: String
            ExprFunctionTestCase("null || `\"b\"`", "null"), // 2nd arg: Ion String
            ExprFunctionTestCase("null || `b`", "null"), // 2nd arg: Ion symbol ``
            ExprFunctionTestCase("null || `'b'`", "null"), // 2nd arg: Ion symbol `''`
            ExprFunctionTestCase("null || null", "null"), // 2nd arg: null
            ExprFunctionTestCase("null || missing", "null"), // 2nd arg: missing

            // 1st arg: missing
            ExprFunctionTestCase("missing || 'b'", "null"), // 2nd arg: String
            ExprFunctionTestCase("missing || `\"b\"`", "null"), // 2nd arg: Ion String
            ExprFunctionTestCase("missing || `b`", "null"), // 2nd arg: Ion symbol ``
            ExprFunctionTestCase("missing || `'b'`", "null"), // 2nd arg: Ion symbol `''`
            ExprFunctionTestCase("missing || null", "null"), // 2nd arg: null
            ExprFunctionTestCase("missing || missing", "null"), // 2nd arg: missing

            // Test for more characters in strings
            ExprFunctionTestCase("'' || 'a'", "\"a\""),
            ExprFunctionTestCase("`'ab'` || `'c'`", "\"abc\""),
            ExprFunctionTestCase("'abcdefghijklmnopqrstuvwxy' || `'z'`", "\"abcdefghijklmnopqrstuvwxyz\""),
            ExprFunctionTestCase("'ȴȵ💩💋' || 'abc'", "\"ȴȵ💩💋abc\""),
            ExprFunctionTestCase("'😁😞😸😸' || 'abc'", "\"😁😞😸😸abc\""),
            ExprFunctionTestCase("'話家身圧費谷料村能' || '計税金'", "\"話家身圧費谷料村能計税金\""),
            ExprFunctionTestCase("'eࠫ' || 'abc'", "\"eࠫabc\""),
        )
    }

    // Error test cases: Invalid argument type
    data class InvalidArgTypeTestCase(
        val source: String,
        val actualArgType: String,
        val line: Long,
        val column: Long,
    )

    @ParameterizedTest
    @ArgumentsSource(InvalidArgTypeCases::class)
    fun concatInvalidArgumentTypeTests(testCase: InvalidArgTypeTestCase) = runEvaluatorErrorTestCase(
        query = testCase.source,
        expectedErrorCode = ErrorCode.EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE,
        expectedErrorContext = propertyValueMapOf(
            testCase.line.toIntExact(), testCase.column.toIntExact(),
            Property.ACTUAL_ARGUMENT_TYPES to testCase.actualArgType
        ),
        expectedPermissiveModeResult = "MISSING"
    )

    class InvalidArgTypeCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // 1st argument wrong
            InvalidArgTypeTestCase("1 || 'a'", listOf("INT", "STRING").toString(), 1L, 3L),
            InvalidArgTypeTestCase("`1` || 'a'", listOf("INT", "STRING").toString(), 1L, 5L),
            InvalidArgTypeTestCase("1.0 || 'a'", listOf("DECIMAL", "STRING").toString(), 1L, 5L),
            InvalidArgTypeTestCase("`1.0` || 'a'", listOf("DECIMAL", "STRING").toString(), 1L, 7L),
            InvalidArgTypeTestCase("`2021T` || 'a'", listOf("TIMESTAMP", "STRING").toString(), 1L, 9L),
            InvalidArgTypeTestCase("<<>> || 'a'", listOf("BAG", "STRING").toString(), 1L, 6L),
            InvalidArgTypeTestCase("sexp() || 'a'", listOf("SEXP", "STRING").toString(), 1L, 8L),
            InvalidArgTypeTestCase("`()` || 'a'", listOf("SEXP", "STRING").toString(), 1L, 6L),
            InvalidArgTypeTestCase("[] || 'a'", listOf("LIST", "STRING").toString(), 1L, 4L),
            InvalidArgTypeTestCase("`[]` || 'a'", listOf("LIST", "STRING").toString(), 1L, 6L),
            InvalidArgTypeTestCase("{} || 'a'", listOf("STRUCT", "STRING").toString(), 1L, 4L),
            InvalidArgTypeTestCase("`{}` || 'a'", listOf("STRUCT", "STRING").toString(), 1L, 6L),
            // 2nd argument wrong
            InvalidArgTypeTestCase("'a' || 1", listOf("STRING", "INT").toString(), 1L, 5L),
            InvalidArgTypeTestCase("'a' || `1`", listOf("STRING", "INT").toString(), 1L, 5L),
            InvalidArgTypeTestCase("'a' || `2021T`", listOf("STRING", "TIMESTAMP").toString(), 1L, 5L),
            InvalidArgTypeTestCase("'a' || <<>>", listOf("STRING", "BAG").toString(), 1L, 5L),
            InvalidArgTypeTestCase("'a' || sexp()", listOf("STRING", "SEXP").toString(), 1L, 5L),
            InvalidArgTypeTestCase("'a' || `()`", listOf("STRING", "SEXP").toString(), 1L, 5L),
            InvalidArgTypeTestCase("'a' || []", listOf("STRING", "LIST").toString(), 1L, 5L),
            InvalidArgTypeTestCase("'a' || `[]`", listOf("STRING", "LIST").toString(), 1L, 5L),
            InvalidArgTypeTestCase("'a' || {}", listOf("STRING", "STRUCT").toString(), 1L, 5L),
            InvalidArgTypeTestCase("'a' || `{}`", listOf("STRING", "STRUCT").toString(), 1L, 5L),
            // both arguments wrong
            InvalidArgTypeTestCase("1 || 1", listOf("INT", "INT").toString(), 1L, 3L),
            InvalidArgTypeTestCase("`1` || `1`", listOf("INT", "INT").toString(), 1L, 5L),
            InvalidArgTypeTestCase("`2021T` || `2021T`", listOf("TIMESTAMP", "TIMESTAMP").toString(), 1L, 9L),
            InvalidArgTypeTestCase("<<>> || <<>>", listOf("BAG", "BAG").toString(), 1L, 6L),
            InvalidArgTypeTestCase("sexp() || sexp()", listOf("SEXP", "SEXP").toString(), 1L, 8L),
            InvalidArgTypeTestCase("`()` || `()`", listOf("SEXP", "SEXP").toString(), 1L, 6L),
            InvalidArgTypeTestCase("[] || []", listOf("LIST", "LIST").toString(), 1L, 4L),
            InvalidArgTypeTestCase("`[]` || `[]`", listOf("LIST", "LIST").toString(), 1L, 6L),
            InvalidArgTypeTestCase("{} || {}", listOf("STRUCT", "STRUCT").toString(), 1L, 4L),
            InvalidArgTypeTestCase("`{}` || `{}`", listOf("STRUCT", "STRUCT").toString(), 1L, 6L)
        )
    }

    // For invalid arity error tests, if anything missing from left side or right side of `||`, it should be a syntax error.
}
