package org.partiql.plan.debug

import org.partiql.plan.ir.PlanNode
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.visitor.PlanBaseVisitor

/**
 * Basic printer for debugging during early development lifecycle
 *
 * Useful for debugging while the Jackson Poem doesn't handle map serde.
 */
object PlanPrinter {

    fun toString(plan: PlanNode): String = buildString { append(this, plan) }

    fun append(out: Appendable, plan: PlanNode) {
        val ctx = Args(out)
        Visitor.visit(plan, ctx)
    }

    // args for a visitor invocation
    private class Args(
        val out: Appendable,
        val levels: Array<Boolean> = emptyArray(),
        val last: Boolean = true,
    ) {
        // leading characters of a tree print
        val lead: String = when (levels.size) {
            0 -> "⚬"
            else -> {
                val prefix = levels.joinToString("") { if (it) "│  " else "   " }
                val suffix = if (last) "├──" else "└──"
                prefix + suffix
            }
        }
    }

    private object Visitor : PlanBaseVisitor<Unit, Args>() {

        private val EOL = System.lineSeparator()

        override fun defaultReturn(node: PlanNode, ctx: Args) = Unit

        override fun defaultVisit(node: PlanNode, ctx: Args): Unit = with(ctx) {
            out.append(lead)
            out.append(' ')
            // print node name
            out.append(node::class.simpleName)
            // TODO print primitive properties
            out.append(EOL)
            // print children, relations last
            node.children.sortedWith(relLast).forEachIndexed { i, child ->
                val args = Args(out, levels + !last, last = i == node.children.size - 1)
                child.accept(Visitor, args)
            }
        }

        val relLast = Comparator<PlanNode> { a, b ->
            when {
                a is Rel && b !is Rel -> -1
                b is Rel && a !is Rel -> 1
                else -> 0
            }
        }
    }
}
