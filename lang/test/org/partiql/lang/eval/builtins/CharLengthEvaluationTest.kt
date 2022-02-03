package org.partiql.lang.eval.builtins

import org.junit.Test
import org.partiql.lang.eval.EvaluatorTestBase

class CharLengthEvaluationTest : EvaluatorTestBase() {
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
}
