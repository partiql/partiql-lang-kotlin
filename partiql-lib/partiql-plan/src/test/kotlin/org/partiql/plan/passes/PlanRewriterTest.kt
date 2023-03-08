package org.partiql.plan.passes

import org.junit.jupiter.api.Test
import org.partiql.plan.debug.PlanPrinter
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.builder.PlanBuilder
import org.partiql.plan.ir.builder.RelScanBuilder
import org.partiql.plan.ir.builder.RexBinaryBuilder
import org.partiql.plan.ir.builder.RexIdBuilder

/**
 * Test which shows rewriting a plan by flipping binary expressions
 */
internal class PlanRewriterTest {

    @Test
    internal fun rewriterExample() {

        val default = Common(
            schema = emptyMap(),
            properties = emptySet(),
            metas = emptyMap(),
        )

        val before = PlanBuilder().relFilter {
            common(default)
            condition(
                RexBinaryBuilder()
                    .lhs(
                        RexBinaryBuilder()
                            .lhs(
                                RexIdBuilder()
                                    .name("x")
                                    .qualifier(Rex.Id.Qualifier.LOCALS_FIRST)
                                    .build()
                            )
                            .rhs(
                                RexIdBuilder()
                                    .name("y")
                                    .qualifier(Rex.Id.Qualifier.LOCALS_FIRST)
                                    .build()
                            )
                            .op(Rex.Binary.Op.TIMES)
                            .build()
                    )
                    .rhs(
                        RexIdBuilder()
                            .name("z")
                            .qualifier(Rex.Id.Qualifier.LOCALS_FIRST)
                            .build()
                    )
                    .op(Rex.Binary.Op.DIV)
                    .build()
            )
            input(
                RelScanBuilder()
                    .common(default)
                    .value(
                        RexIdBuilder()
                            .name("foo")
                            .qualifier(Rex.Id.Qualifier.LOCALS_FIRST)
                            .build()
                    )
                    .build()
            )
        }
        // SCAN 'foo' |> FILTER (x * y) / z
        PlanPrinter.append(System.out, before)

        // SCAN 'foo' |> FILTER z / (y * z)
        val after = before.accept(BinaryFlip, Unit)
        PlanPrinter.append(System.out, after)
    }

    private object BinaryFlip : PlanRewriter() {

        override fun visitRexBinary(node: Rex.Binary, ctx: Unit) = Rex.Binary(
            lhs = node.rhs.accept(this, ctx) as Rex,
            rhs = node.lhs.accept(this, ctx) as Rex,
            op = node.op,
            type = null
        )
    }
}
