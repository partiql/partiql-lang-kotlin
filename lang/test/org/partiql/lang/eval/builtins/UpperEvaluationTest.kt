package org.partiql.lang.eval.builtins

import org.junit.Test
import org.partiql.lang.eval.EvaluatorTestBase

class UpperEvaluationTest : EvaluatorTestBase() {
    @Test fun upper_0() = assertEval("upper('')", "\"\"")
    @Test fun upper_1() = assertEval("upper('abcdef')", "\"ABCDEF\"")
    @Test fun upper_2() = assertEval("upper('ABCDEF')", "\"ABCDEF\"")
    @Test fun upper_3() = assertEval("upper(null)", "null")
    @Test fun upper_4() = assertEval("upper(missing)", "null")
    @Test fun upper_5() = assertEval("upper('123\$%(*&')", "\"123\$%(*&\"")
    @Test fun upper_6() = assertEval("upper('ȴȵ💩z💋')", "\"ȴȵ💩Z💋\"")
    @Test fun upper_7() = assertEval("upper('話家身圧費谷料村能計税金')", "\"話家身圧費谷料村能計税金\"")
}
