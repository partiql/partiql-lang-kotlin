package com.amazon.ionsql

import com.amazon.ionsql.eval.EvaluationException
import com.amazon.ionsql.eval.EvaluatorBase
import org.junit.Test

class BuiltinFunctionsTest : EvaluatorBase() {

    // Old syntax: SUBSTRING(<str> FROM <start pos> [FOR <length>])
    @Test fun substringOldSyntax_1() = assertEval("substring('abcdefghi' from 0)", "\"abcdefghi\"")
    @Test fun substringOldSyntax_2() = assertEval("substring('abcdefghi' from 1)", "\"abcdefghi\"")
    @Test fun substringOldSyntax_3() = assertEval("substring('abcdefghi' from -1)", "\"abcdefghi\"")
    @Test fun substringOldSyntax_4() = assertEval("substring('abcdefghi' from 3)", "\"cdefghi\"")
    @Test fun substringOldSyntax_5() = assertEval("substring('abcdefghi' from 3 for 4)", "\"cdef\"")
    @Test fun substringOldSyntax_6() = assertEval("substring('abcdefghi' from -1 for 4)", "\"ab\"")
    @Test fun substringOldSyntax_7() = assertEval("substring('abcdefghi' from 1 for 1)", "\"a\"")
    @Test(expected = EvaluationException::class)
    fun substringOldSyntax_8() = assertEval("substring(123456789 from 1 for 3)", "\"123\"")
    @Test(expected = EvaluationException::class)
    fun substringOldSyntax_9() = assertEval("substring('abcdefghi' from 1 for -1)", "\"a\"")

    @Test fun substringOldSyntaxNullArg_1() = assertEval("substring(null from 1)", "null")
    @Test fun substringOldSyntaxNullArg_2() = assertEval("substring('abc' from null)", "null")
    @Test fun substringOldSyntaxNullArg_3() = assertEval("substring(null from 1 for 1)", "null")
    @Test fun substringOldSyntaxNullArg_4() = assertEval("substring('abc' from null for 1)", "null")
    @Test fun substringOldSyntaxNullArg_5() = assertEval("substring('abc' from 1 for null)", "null")

    @Test fun substringOldSyntaxMissingArg_1() = assertEval("substring(missing from 1)", "null")
    @Test fun substringOldSyntaxMissingArg_2() = assertEval("substring('abc' from missing)", "null")
    @Test fun substringOldSyntaxMissingArg_3() = assertEval("substring(missing from 1 for 1)", "null")
    @Test fun substringOldSyntaxMissingArg_4() = assertEval("substring('abc' from missing for 1)", "null")
    @Test fun substringOldSyntaxMissingArg_5() = assertEval("substring('abc' from 1 for missing)", "null")

    //Same as above, with the new syntax: SUBSTRING(<str>, <start pos> [, <length>])
    @Test fun substringNewSyntax_1() = assertEval("substring('abcdefghi', 0)", "\"abcdefghi\"")
    @Test fun substringNewSyntax_2() = assertEval("substring('abcdefghi', 1)", "\"abcdefghi\"")
    @Test fun substringNewSyntax_3() = assertEval("substring('abcdefghi', -1)", "\"abcdefghi\"")
    @Test fun substringNewSyntax_4() = assertEval("substring('abcdefghi', 3)", "\"cdefghi\"")
    @Test fun substringNewSyntax_5() = assertEval("substring('abcdefghi', 3, 4)", "\"cdef\"")
    @Test fun substringNewSyntax_6() = assertEval("substring('abcdefghi', -1, 4)", "\"ab\"")
    @Test fun substringNewSyntax_7() = assertEval("substring('abcdefghi', 1, 1)", "\"a\"")
    @Test(expected = EvaluationException::class)
    fun substringNewSyntax_8() = assertEval("substring(123456789, 1, 3)", "\"123\"")
    @Test(expected = EvaluationException::class)
    fun substringNewSyntax_9() = assertEval("substring('abcdefghi', 1, -1)", "\"a\"")

    @Test fun substringNewSyntaxNullArg_1() = assertEval("substring(null, 1)", "null")
    @Test fun substringNewSyntaxNullArg_2() = assertEval("substring('abc', null)", "null")
    @Test fun substringNewSyntaxNullArg_3() = assertEval("substring(null, 1, 1)", "null")
    @Test fun substringNewSyntaxNullArg_4() = assertEval("substring('abc', null, 1)", "null")
    @Test fun substringNewSyntaxNullArg_5() = assertEval("substring('abc', 1, null)", "null")

    @Test fun substringNewSyntaxMissingArg_1() = assertEval("substring(missing, 1)", "null")
    @Test fun substringNewSyntaxMissingArg_2() = assertEval("substring('abc', missing)", "null")
    @Test fun substringNewSyntaxMissingArg_3() = assertEval("substring(missing, 1, 1)", "null")
    @Test fun substringNewSyntaxMissingArg_4() = assertEval("substring('abc', missing, 1)", "null")
    @Test fun substringNewSyntaxMissingArg_5() = assertEval("substring('abc', 1, missing)", "null")
}