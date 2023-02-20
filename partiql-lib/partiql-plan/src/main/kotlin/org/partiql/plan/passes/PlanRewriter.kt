package org.partiql.plan.passes

import org.partiql.plan.ir.PlanNode
import org.partiql.plan.ir.visitor.PlanBaseVisitor
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Simple plan rewriter.
 *
 * Here are some issues with this:
 * 1. We should generate this, because "reflection is slow", but also this is hacky
 * 2. This assumes there's a primary constructor where each parameter is a member property
 * 3. This will not rewrite collections of nodes
 */
abstract class PlanRewriter : PlanBaseVisitor<PlanNode, Unit>() {

    override fun defaultReturn(node: PlanNode, ctx: Unit) = node

    override fun defaultVisit(node: PlanNode, ctx: Unit): PlanNode {
        var hadChange = false
        // constructor params
        val constructor = node::class.primaryConstructor!!
        val props = node.javaClass.kotlin.declaredMemberProperties
        // rewrite props, traverse constructor params to get proper argument order
        val args = constructor.parameters.mapNotNull { para ->
            val prop = props.find { prop -> prop.name == para.name } ?: return@mapNotNull null
            var arg: Any? = prop.get(node)
            if (arg is PlanNode) {
                val child = arg.accept(this, Unit)
                if (child != arg) {
                    arg = child
                    hadChange = true
                }
            }
            arg
        }
        // instantiate new node if any child changed
        return if (hadChange) constructor.call(*args.toTypedArray()) else node
    }
}
