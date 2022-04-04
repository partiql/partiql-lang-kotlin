package org.partiql.lang.ast.passes

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestCasesAsExprNodeTestCases
import org.partiql.lang.util.testdsl.ExprNodeTestCase

/**
 * This test class verifies that [AstRewriterBase.rewriteExprNode] returns an exact copy of the original.
 *
 * This unfortunately can't verify that a deep copy was made of all nodes, so it is still
 * possible that [AstRewriterBase] might inadvertently return a shallow copy somewhere. That is hard to
 * verify.
 *
 * TODO:  when SqlParserTest case has been parameterized, include its test cases here too.
 */
class AstRewriterBaseTest {
    @Suppress("DEPRECATION")
    private val defaultRewriter = AstRewriterBase()
    @ParameterizedTest
    @ArgumentsSource(EvaluatorTestCasesAsExprNodeTestCases::class)
    fun identityRewriteTest(tc: ExprNodeTestCase) {
        val rewritten = defaultRewriter.rewriteExprNode(tc.expr)
        tc.assertEquals(rewritten)
    }
}
