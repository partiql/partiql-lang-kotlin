package org.partiql.plan.visitor

import org.partiql.plan.PlanNode
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Simple plan rewriter.
 *
 * THIS HAS ISSUES!
 * 1. We should generate this, because "reflection is slow", but also this is hacky
 * 2. This assumes there's a primary constructor where each parameter is a member property
 * 3. This will not rewrite collections of nodes
 *
 *  - Function Lowering
 *  - Constant Folding
 *  - Reduce Logical Expressions
 *  - Remove always true filters
 *  - Replace always false filters with empty query
 *  - Proj/Pred push down
 */
abstract class PlanRewriter<T> : PlanBaseVisitor<PlanNode, T>() {

    override fun defaultReturn(node: PlanNode, ctx: T) = node

    override fun defaultVisit(node: PlanNode, ctx: T): PlanNode {
        var hadChange = false
        // constructor params
        val constructor = node::class.primaryConstructor!!
        val props = node.javaClass.kotlin.declaredMemberProperties
        // rewrite props, traverse constructor params to get proper argument order
        val args = constructor.parameters.mapNotNull { para ->
            val prop = props.find { prop -> prop.name == para.name } ?: return@mapNotNull null
            var arg: Any? = prop.get(node)
            if (arg is PlanNode) {
                val child = arg.accept(this, ctx)
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
