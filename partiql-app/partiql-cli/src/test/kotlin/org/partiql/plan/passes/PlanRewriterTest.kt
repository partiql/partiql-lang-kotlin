package org.partiql.plan.passes

import org.junit.jupiter.api.Test
import org.partiql.plan.debug.PlanPrinter
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.builder.PlanBuilder

/**
 * Test which shows rewriting a plan by flipping binary expressions
 */
internal class PlanRewriterTest {

    @Test
    internal fun rewriterExample() {

        val default = Common(
            schema = emptyList(),
            properties = emptySet(),
            metas = emptyMap(),
        )

        val before = PlanBuilder.build {
            relFilter {
                common = default
                condition = rexBinary {
                    lhs = rexBinary {
                        lhs = rexId("x", qualifier = Rex.Id.Qualifier.UNQUALIFIED)
                        rhs = rexId("y", qualifier = Rex.Id.Qualifier.UNQUALIFIED)
                        op = Rex.Binary.Op.TIMES
                    }
                    rhs = rexId("z", qualifier = Rex.Id.Qualifier.UNQUALIFIED)
                    op = Rex.Binary.Op.DIV
                }
                input = relScan {
                    common = default
                    value = rexId("foo", qualifier = Rex.Id.Qualifier.UNQUALIFIED)
                }
            }
        }

        // SCAN 'foo' |> FILTER (x * y) / z
        PlanPrinter.append(System.out, before)

        // SCAN 'foo' |> FILTER z / (y * z)
        val after = before.accept(BinaryFlip, Unit)
        PlanPrinter.append(System.out, after)
    }

    private object BinaryFlip : PlanRewriter<Unit>() {

        override fun visitRexBinary(node: Rex.Binary, ctx: Unit) = Rex.Binary(
            lhs = node.rhs.accept(this, ctx) as Rex,
            rhs = node.lhs.accept(this, ctx) as Rex,
            op = node.op,
        )
    }
}
