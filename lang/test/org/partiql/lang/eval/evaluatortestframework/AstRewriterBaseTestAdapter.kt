@file: Suppress("DEPRECATION") // suppress warnings about ExprNode deprecation.
package org.partiql.lang.eval.evaluatortestframework

import org.partiql.lang.CUSTOM_TEST_TYPES
import org.partiql.lang.ION
import org.partiql.lang.ast.passes.AstRewriterBase
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.syntax.SqlParser
import kotlin.test.assertEquals

/** Tests [org.partiql.lang.ast.passes.AstRewriterBase]. */
class AstRewriterBaseTestAdapter : EvaluatorTestAdapter {
    private val defaultRewriter = AstRewriterBase()

    override fun runEvaluatorTestCase(tc: EvaluatorTestCase, session: EvaluationSession) {
        testAstRewriterBase(tc)
    }

    override fun runEvaluatorErrorTestCase(tc: EvaluatorErrorTestCase, session: EvaluationSession) {
        testAstRewriterBase(tc)
    }

    private fun testAstRewriterBase(tc: EvaluatorTestDefinition) {
        val parser = SqlParser(ION, CUSTOM_TEST_TYPES)
        val ast = parser.parseAstStatement(tc.query)

        val exprNode = ast.toExprNode(ION)

        val clonedAst = defaultRewriter.rewriteExprNode(exprNode)
        assertEquals(
            exprNode,
            clonedAst,
            "AST returned from default AstRewriterBase should match the original AST. SQL was: ${tc.query}"
        )
    }
}
