package com.amazon.ionsql.eval.builtins

import com.amazon.ionsql.eval.*
import org.junit.*

class BuiltinFunctionsTest : EvaluatorBase() {

    // Old syntax: SUBSTRING(<str> FROM <start pos> [FOR <length>])
    @Test fun substringOldSyntax_1() = assertEval("substring('abcdefghi' from 0)", "\"abcdefghi\"")
    @Test fun substringOldSyntax_2() = assertEval("substring('abcdefghi' from 1)", "\"abcdefghi\"")
    @Test fun substringOldSyntax_3() = assertEval("substring('abcdefghi' from -1)", "\"abcdefghi\"")
    @Test fun substringOldSyntax_4() = assertEval("substring('abcdefghi' from 3)", "\"cdefghi\"")
    @Test fun substringOldSyntax_5() = assertEval("substring('abcdefghi' from 3 for 4)", "\"cdef\"")
    @Test fun substringOldSyntax_6() = assertEval("substring('abcdefghi' from -1 for 4)", "\"ab\"")
    @Test fun substringOldSyntax_7() = assertEval("substring('abcdefghi' from 1 for 1)", "\"a\"")
    @Test fun substringOldSyntax_8() = assertEval("substring('😁😞😸😸' from 2 for 2)", "\"😞😸\"")
    @Test fun substringOldSyntax_9() = assertEval("substring('話家身圧費谷料村能計税金' from 3 for 5)", "\"身圧費谷料\"")
    @Test fun substringOldSyntax_10() = assertEval("substring('話家身圧費谷料村能計税金' from -3 for 6)", "\"話家\"")
    //Note:  U+0832 is a "combining diacritical mark" https://en.wikipedia.org/wiki/Combining_character.
    //Even though it is visually merged with the preceding letter when displayed, it still counts as a distinct codepoint.
    @Test fun substringOldSyntax_11() = assertEval("substring('abcde\u0832fgh' from 3 for 6)", "\"cde\u0832fg\"")

    @Test(expected = EvaluationException::class)
    fun substringOldSyntax_12() = assertEval("substring(123456789 from 1 for 3)", "\"123\"")
    @Test(expected = EvaluationException::class)
    fun substringOldSyntax_13() = assertEval("substring('abcdefghi' from 1 for -1)", "\"a\"")

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
    @Test fun substringNewSyntax_8() = assertEval("substring('😁😞😸😸', 2, 2)", "\"😞😸\"")
    @Test fun substringNewSyntax_9() = assertEval("substring('話家身圧費谷料村能計税金', 3, 5)", "\"身圧費谷料\"")
    @Test fun substringNewSyntax_10() = assertEval("substring('話家身圧費谷料村能計税金', -3, 6)", "\"話家\"")
    @Test fun substringNewSyntax_11() = assertEval("substring('abcde\u0832fgh', 3, 6)", "\"cde\u0832fg\"")

    @Test(expected = EvaluationException::class)
    fun substringNewSyntax_12() = assertEval("substring(123456789, 1, 3)", "\"123\"")
    @Test(expected = EvaluationException::class)
    fun substringNewSyntax_13() = assertEval("substring('abcdefghi', 1, -1)", "\"a\"")

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

    //Note: character_length is same as char_length
    @Test fun character_length_1() = assertEval("character_length('a')", "1")
    @Test fun char_length_0() = assertEval("char_length('')", "0")
    @Test fun char_length_1() = assertEval("char_length('a')", "1")
    @Test fun char_length_2() = assertEval("char_length('ab')", "2")
    @Test fun char_length_3() = assertEval("char_length('abcdefghijklmnopqrstuvwxyz')", "26")
    @Test fun char_length_4() = assertEval("char_length(null)", "null")
    @Test fun char_length_5() = assertEval("char_length(missing)", "null")
    @Test fun char_length_6() = assertEval("char_length('ȴȵ💩💋')", "4")
    @Test fun char_length_7() = assertEval("char_length('😁😞😸😸')", "4")
    @Test fun char_length_8() = assertEval("char_length('話家身圧費谷料村能計税金')", "12")
    @Test fun char_length_9() = assertEval("char_length('eࠫ')", "2") //This is a unicode "combining character" which is actually 2 codepoints

    @Test fun upper_0() = assertEval("upper('')", "\"\"")
    @Test fun upper_1() = assertEval("upper('abcdef')", "\"ABCDEF\"")
    @Test fun upper_2() = assertEval("upper('ABCDEF')", "\"ABCDEF\"")
    @Test fun upper_3() = assertEval("upper(null)", "null")
    @Test fun upper_4() = assertEval("upper(missing)", "null")
    @Test fun upper_5() = assertEval("upper('123\$%(*&')", "\"123\$%(*&\"")
    @Test fun upper_6() = assertEval("upper('ȴȵ💩z💋')", "\"ȴȵ💩Z💋\"")
    @Test fun upper_7() = assertEval("upper('話家身圧費谷料村能計税金')", "\"話家身圧費谷料村能計税金\"")

    @Test fun lower_0() = assertEval("lower('')", "\"\"")
    @Test fun lower_1() = assertEval("lower('ABCDEF')", "\"abcdef\"")
    @Test fun lower_2() = assertEval("lower('abcdef')", "\"abcdef\"")
    @Test fun lower_3() = assertEval("lower(null)", "null")
    @Test fun lower_4() = assertEval("lower(missing)", "null")
    @Test fun lower_5() = assertEval("lower('123\$%(*&')", "\"123\$%(*&\"")
    @Test fun lower_6() = assertEval("lower('ȴȵ💩Z💋')", "\"ȴȵ💩z💋\"")
    @Test fun lower_7() = assertEval("lower('話家身圧費谷料村能計税金')", "\"話家身圧費谷料村能計税金\"")
}
