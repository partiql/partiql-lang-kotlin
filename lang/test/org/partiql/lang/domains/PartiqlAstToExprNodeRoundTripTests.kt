
@file:Suppress("DEPRECATION") // We don't need warnings about ExprNode deprecation.

package org.partiql.lang.domains

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ION
import org.partiql.lang.ast.passes.MetaStrippingRewriter
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.eval.EvaluatorTestCasesAsExprNodeTestCases
import org.partiql.lang.util.testdsl.ExprNodeTestCase

class PartiqlAstToExprNodeRoundTripTests {
    // TODO: when SqlParserTest has been parameterized, extract more [ExprNodeTestCases] from its test cases
    // and include them in this test.
    @ParameterizedTest
    @ArgumentsSource(EvaluatorTestCasesAsExprNodeTestCases::class)
    fun roundTripTests(tc: ExprNodeTestCase) {
        // TODO:  don't strip these metas, it's preventing this test from being as thorough as it could be.
        val stripped = MetaStrippingRewriter.stripMetas(tc.expr)
        val roundTrippedExprNode = stripped.toAstStatement().toExprNode(ION)
        assertEquals(stripped, roundTrippedExprNode)
    }
}
