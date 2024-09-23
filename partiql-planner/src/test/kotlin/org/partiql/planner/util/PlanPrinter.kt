package org.partiql.planner.util

import org.partiql.plan.v1.PartiQLPlan
import org.partiql.types.PType

/**
 * Basic printer for debugging during early development lifecycle
 *
 * Useful for debugging while the Jackson Poem doesn't handle map serde.
 */
public object PlanPrinter {

    fun toString(plan: PartiQLPlan): String = buildString { append(this, plan) }

    fun append(out: Appendable, plan: PartiQLPlan) {
        out.append("TODO PRINTING OF PLANS FOR TEST DEBUGGING")
        // val ctx = Args(out)
        // Visitor.visit(plan, ctx)
    }

    // args for a visitor invocation
    private data class Args(
        val out: Appendable,
        val levels: Array<Boolean> = emptyArray(),
        val last: Boolean = true,
        val type: TypeInfo = TypeInfo.Nil,
    ) {
        sealed interface TypeInfo {
            class Rel(val type: org.partiql.plan.Rel.Type) : TypeInfo
            class Rex(val type: PType) : TypeInfo
            object Nil : TypeInfo
        }

        // leading characters of a tree print
        val lead: String = when (levels.size) {
            0 -> "⚬ "
            else -> {
                val prefix = levels.joinToString("") { if (it) "│  " else "   " }
                val suffix = if (last) "└── " else "├── "
                prefix + suffix
            }
        }
    }

    // private object Visitor : PlanBaseVisitor<Unit, Args>() {
    //
    //     private val EOL = System.lineSeparator()
    //
    //     private val relLast = Comparator<PlanNode> { a, b ->
    //         when {
    //             a is Rel && b !is Rel -> 1
    //             b is Rel && a !is Rel -> -1
    //             else -> 0
    //         }
    //     }
    //
    //     override fun defaultReturn(node: PlanNode, ctx: Args) = Unit
    //
    //     override fun visitRel(node: Rel, ctx: Args) = with(ctx) {
    //         visitRelOp(node.op, ctx = ctx.copy(type = Args.TypeInfo.Rel(node.type)))
    //     }
    //
    //     override fun visitRex(node: Rex, ctx: Args) {
    //         visitRexOp(node.op, ctx = ctx.copy(type = Args.TypeInfo.Rex(node.type)))
    //     }
    //
    //     private fun Args.TypeInfo.args(): List<Pair<String, Any>> {
    //         return when (this) {
    //             is Args.TypeInfo.Rel -> return listOf(
    //                 "schema" to this.type.schema,
    //                 "props" to this.type.props
    //             )
    //             is Args.TypeInfo.Rex -> return listOf(
    //                 "static_type" to this.type.toString()
    //             )
    //             is Args.TypeInfo.Nil -> emptyList()
    //         }
    //     }
    //
    //     override fun defaultVisit(node: PlanNode, ctx: Args): Unit = with(ctx) {
    //         out.append(lead)
    //         // print node name
    //         out.append(node::class.simpleName)
    //         // print primitive items
    //         val primitives = node.primitives().filter { it.second != null } + type.args()
    //         if (primitives.isNotEmpty()) {
    //             out.append("[")
    //             out.append(primitives.joinToString { "${it.first}=${it.second}" })
    //             out.append("]")
    //         }
    //         out.append(EOL)
    //         // print child nodes
    //         val children = node.children.sortedWith(relLast)
    //         children.forEachIndexed { i, child ->
    //             val args = Args(out, levels + !last, last = i == children.size - 1)
    //             child.accept(Visitor, args)
    //         }
    //     }
    //
    //     // joins all primitive properties as strings [ (<k_0, v_0>), ... (<k_n>, <v_n>) ]
    //     private fun PlanNode.primitives(): List<Pair<String, Any?>> = javaClass.kotlin.memberProperties
    //         .filter {
    //             val t = it.returnType.jvmErasure
    //             val notChildrenOrId = it.name != "children" && it.name != "_id"
    //             val notNode = !t.isSubclassOf(PlanNode::class)
    //             // not currently correct
    //             val notCollectionOfNodes = !(t.isSubclassOf(Collection::class))
    //             notChildrenOrId && notNode && notCollectionOfNodes && it.visibility == KVisibility.PUBLIC
    //         }
    //         .map { it.name to it.get(this) }
    // }
}
