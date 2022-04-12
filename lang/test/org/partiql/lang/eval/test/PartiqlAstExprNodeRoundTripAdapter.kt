package org.partiql.lang.eval.test

import org.partiql.lang.CUSTOM_TEST_TYPES
import org.partiql.lang.ION
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.syntax.SqlParser
import kotlin.test.assertEquals

/** Tests [toExprNode] and [toAstStatement]. */
class PartiqlAstExprNodeRoundTripAdapter : EvaluatorTestAdapter {

    override fun runEvaluatorTestCase(tc: EvaluatorTestCase, session: EvaluationSession) {
        runRoundTripTest(tc)
    }

    override fun runEvaluatorErrorTestCase(tc: EvaluatorErrorTestCase, session: EvaluationSession) {
        runRoundTripTest(tc)
    }

    private fun runRoundTripTest(tc: EvaluatorTestDefinition) {
        val parser = SqlParser(ION, CUSTOM_TEST_TYPES)
        val ast = parser.parseAstStatement(tc.query)

        val roundTrippedAst = ast.toExprNode(ION).toAstStatement()
        assertEquals(
            ast,
            roundTrippedAst,
            "PIG ast resulting from round trip to ExprNode and back should be equivalent."
        )
    }
}
