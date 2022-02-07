package org.partiql.lang.eval.builtins

import org.junit.Test
import org.partiql.lang.eval.EvaluatorTestBase

class LowerEvaluationTest : EvaluatorTestBase() {
    @Test fun lower_0() = assertEval("lower('')", "\"\"")
    @Test fun lower_1() = assertEval("lower('ABCDEF')", "\"abcdef\"")
    @Test fun lower_2() = assertEval("lower('abcdef')", "\"abcdef\"")
    @Test fun lower_3() = assertEval("lower(null)", "null")
    @Test fun lower_4() = assertEval("lower(missing)", "null")
    @Test fun lower_5() = assertEval("lower('123\$%(*&')", "\"123\$%(*&\"")
    @Test fun lower_6() = assertEval("lower('ȴȵ💩Z💋')", "\"ȴȵ💩z💋\"")
    @Test fun lower_7() = assertEval("lower('話家身圧費谷料村能計税金')", "\"話家身圧費谷料村能計税金\"")
}
