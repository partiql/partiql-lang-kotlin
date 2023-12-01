package org.partiql.plan.debug

import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.visitor.PlanBaseVisitor
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

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
            0 -> "⚬ "
            else -> {
                val prefix = levels.joinToString("") { if (it) "│  " else "   " }
                val suffix = if (last) "└──" else "├──"
                prefix + suffix
            }
        }
    }

    private object Visitor : PlanBaseVisitor<Unit, Args>() {

        private val EOL = System.lineSeparator()

        private val relLast = Comparator<PlanNode> { a, b ->
            when {
                a is Rel && b !is Rel -> 1
                b is Rel && a !is Rel -> -1
                else -> 0
            }
        }

        override fun defaultReturn(node: PlanNode, ctx: Args) = Unit

        override fun defaultVisit(node: PlanNode, ctx: Args): Unit = with(ctx) {
            out.append(lead)
            // print node name
            out.append(node::class.simpleName)
            // print primitive items
            val primitives = node.primitives().filter { it.second != null }
            if (primitives.isNotEmpty()) {
                out.append("[")
                out.append(primitives.joinToString { "${it.first}=${it.second}" })
                out.append("]")
            }
            out.append(EOL)
            // print child nodes
            val children = node.children.sortedWith(relLast)
            children.forEachIndexed { i, child ->
                val args = Args(out, levels + !last, last = i == children.size - 1)
                child.accept(Visitor, args)
            }
        }

        // joins all primitive properties as strings [ (<k_0, v_0>), ... (<k_n>, <v_n>) ]
        private fun PlanNode.primitives(): List<Pair<String, Any?>> = javaClass.kotlin.memberProperties
            .filter {
                val t = it.returnType.jvmErasure
                val notChildrenOrId = it.name != "children" && it.name != "_id"
                val notNode = !t.isSubclassOf(PlanNode::class)
                // not currently correct
                val notCollectionOfNodes = !(t.isSubclassOf(Collection::class))
                notChildrenOrId && notNode && notCollectionOfNodes && it.visibility == KVisibility.PUBLIC
            }
            .map { it.name to it.get(this) }

        // override fun visitIdentifierSymbol(node: Identifier.Symbol, ctx: Args): Unit = with(ctx) {
        //     out.append(lead)
        //     val sql = when (node.caseSensitivity) {
        //         Identifier.CaseSensitivity.SENSITIVE -> "\"${node.symbol}\""
        //         Identifier.CaseSensitivity.INSENSITIVE -> node.symbol
        //     }
        //     out.append(sql)
        //     out.append(EOL)
        // }
        //
        // override fun visitIdentifierQualified(node: Identifier.Qualified, ctx: Args): Unit = with(ctx) {
        //     out.append(lead)
        //     val root = visitIdentifierSymbol(node.root, ctx)
        //     val args = Args(out, levels + !last, last = false)
        //     val steps = node.steps.map { visitIdentifierSymbol(it, args) }
        //     val sql = (listOf(root) + steps).joinToString(".")
        //     out.append(sql)
        //     out.append(EOL)
        // }
    }
}
