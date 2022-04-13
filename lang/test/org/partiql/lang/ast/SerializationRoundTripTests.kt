
@file:Suppress("DEPRECATION") // We don't need warnings about ExprNode deprecation.

package org.partiql.lang.ast

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ION
import org.partiql.lang.ast.passes.MetaStrippingRewriter
import org.partiql.lang.eval.EVALUATOR_TEST_SUITE
import org.partiql.lang.util.testdsl.ExprNodeTestCase
import kotlin.test.assertEquals

/**
 * Verifies that the all test [ExprNode] instances survive a round trip through [AstSerializer] and [AstDeserializer].
 *
 * TODO: when SqlParserTest has been parameterized, include its test cases here too.
 */
class SerializationRoundTripTests {
    private val deserializer = AstDeserializerBuilder(ION).build()

    companion object {
        @JvmStatic
        @Suppress("UNUSED")
        fun parametersForRoundTripTests() =
            EVALUATOR_TEST_SUITE.getAllTests(
                failingTestNames = hashSetOf(
                    // CAN_CAST is not supported by V0 and will never be.
                    "canCastAsFloat1"
                )
            )
                // we really don't need to test failure cases in this case since the (de)serializers are legacy.
                .filter { !it.expectFailure }
                .map { it.toExprNodeTestCase() }
    }

    @ParameterizedTest
    @MethodSource("parametersForRoundTripTests")
    fun roundTripTests(tc: ExprNodeTestCase) {
        val sexp = assertDoesNotThrow("For test '${tc.name}', serializing the ExprNode should not throw") {
            AstSerializer.serialize(tc.expr, AstVersion.V0, ION)
        }
        val roundTrippedExprNode = assertDoesNotThrow("For test '${tc.name}', deserializing the serialized AST should not throw") {
            deserializer.deserialize(sexp, AstVersion.V0)
        }

        val originalStripped = MetaStrippingRewriter.stripMetas(tc.expr)
        val roundTrippedStripped = MetaStrippingRewriter.stripMetas(roundTrippedExprNode)
        assertEquals(originalStripped, roundTrippedStripped, "ExprNode deserialized from s-exp V0 AST must match the ExprNode")
    }
}
