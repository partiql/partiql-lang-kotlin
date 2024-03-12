package org.partiql.eval.framework.adapter.impl

import org.partiql.eval.CUSTOM_TEST_TYPES
import org.partiql.eval.framework.adapter.EvaluatorTestAdapter
import org.partiql.eval.framework.testcase.EvaluatorTestDefinition
import org.partiql.eval.framework.testcase.impl.EvaluatorErrorTestCase
import org.partiql.eval.framework.testcase.impl.EvaluatorTestCase
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.visitors.VisitorTransformBase
import org.partiql.lang.syntax.PartiQLParserBuilder
import kotlin.test.assertEquals

/** Test that the default visitor transformer returns an exact copy as the original ast */
class VisitorTransformBaseTestAdapter : EvaluatorTestAdapter {
    private val defaultTransformer = object : VisitorTransformBase() {}

    override fun runEvaluatorTestCase(tc: EvaluatorTestCase, session: EvaluationSession) {
        testVistorTransformBase(tc)
    }

    override fun runEvaluatorErrorTestCase(tc: EvaluatorErrorTestCase, session: EvaluationSession) {
        testVistorTransformBase(tc)
    }

    private fun testVistorTransformBase(tc: EvaluatorTestDefinition) {
        val parser = PartiQLParserBuilder.standard()
            .customTypes(CUSTOM_TEST_TYPES)
            .build()
        val ast = parser.parseAstStatement(tc.query)
        val clonedAst = defaultTransformer.transformStatement(ast)
        assertEquals(
            ast,
            clonedAst,
            "AST returned from default visitor transformer should match the original AST. SQL was: ${tc.query}"
        )
    }
}
