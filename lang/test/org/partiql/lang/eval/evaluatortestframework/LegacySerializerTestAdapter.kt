@file:Suppress("DEPRECATION") // deprecation warnings about ExprNode not needed here.

package org.partiql.lang.eval.evaluatortestframework

import org.partiql.lang.CUSTOM_TEST_TYPES
import org.partiql.lang.ION
import org.partiql.lang.ast.AstDeserializer
import org.partiql.lang.ast.AstDeserializerBuilder
import org.partiql.lang.ast.AstSerializer
import org.partiql.lang.ast.AstVersion
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.stripMetas
import kotlin.test.assertEquals

/**
 * Tests the legacy V0 serializer using the AST of the parsed [EvaluatorTestDefinition.query], if
 * [EvaluatorTestDefinition.excludeLegacySerializerAssertions] is `false`.
 */
class LegacySerializerTestAdapter : EvaluatorTestAdapter {
    override fun runEvaluatorTestCase(tc: EvaluatorTestCase, session: EvaluationSession) {
        runSerializerAssertions(tc)
    }

    override fun runEvaluatorErrorTestCase(tc: EvaluatorErrorTestCase, session: EvaluationSession) {
        runSerializerAssertions(tc)
    }

    private fun runSerializerAssertions(tc: EvaluatorTestDefinition) {
        if (!tc.excludeLegacySerializerAssertions) {
            val parser = SqlParser(ION, CUSTOM_TEST_TYPES)
            val exprNode: ExprNode = parser.parseAstStatement(tc.query).toExprNode(ION)
            val deserializer: AstDeserializer = AstDeserializerBuilder(ION).build()
            AstVersion.values().forEach { astVersion ->
                val sexpRepresentation = AstSerializer.serialize(exprNode, astVersion, ION)
                val roundTrippedExprNode = deserializer.deserialize(sexpRepresentation, astVersion)
                assertEquals(
                    exprNode.stripMetas(),
                    roundTrippedExprNode.stripMetas(),
                    "ExprNode deserialized from s-exp $astVersion AST must match the ExprNode returned by the parser"
                )
            }
        }
    }
}
