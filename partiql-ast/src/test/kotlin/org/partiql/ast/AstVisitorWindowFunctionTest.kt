package org.partiql.ast

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * The default [AstVisitor] methods of the argument-less window function types must not recurse infinitely (they used to
 * call `node.accept(this, ctx)`, which dispatches back to the same method).
 */
@Suppress("DEPRECATION")
class AstVisitorWindowFunctionTest {

    @ParameterizedTest
    @MethodSource("windowFunctionTypes")
    fun `default visitor visits window function types`(node: WindowFunctionType) {
        val visitor = object : AstVisitor<String, Unit>() {
            override fun defaultReturn(node: AstNode, ctx: Unit): String = "visited"
        }
        assertEquals("visited", node.accept(visitor, Unit))
    }

    companion object {
        @JvmStatic
        fun windowFunctionTypes() = listOf(
            WindowFunctionType.Rank(),
            WindowFunctionType.DenseRank(),
            WindowFunctionType.PercentRank(),
            WindowFunctionType.CumeDist(),
            WindowFunctionType.RowNumber(),
        )
    }
}
