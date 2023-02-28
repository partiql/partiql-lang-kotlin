package org.partiql.plan.passes

import org.partiql.plan.ir.PlanNode
import org.partiql.plan.ir.visitor.PlanBaseVisitor
import org.partiql.lang.types.StaticType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

abstract class RexTyper<T> : PlanBaseVisitor<StaticType, T>() {

    override fun defaultReturn(node: PlanNode, ctx: T) = StaticType.ANY

    override fun defaultVisit(node: PlanNode, ctx: T): StaticType {
        val constructor = node::class.primaryConstructor!!
        val props = node.javaClass.kotlin.declaredMemberProperties
        var type: StaticType = StaticType.ANY
        constructor.parameters.forEach { para ->
            val prop = props.find { prop -> prop.name == para.name } ?: return@forEach
            val arg: Any? = prop.get(node)
            if (arg is PlanNode) {
                type = arg.accept(this, ctx)
            }
        }
        return type
    }
}
