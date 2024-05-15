package org.partiql.planner.internal.transforms

import org.partiql.planner.internal.ir.Constraint
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.ir.Type
import org.partiql.planner.internal.ir.visitor.PlanBaseVisitor

// DDL is an experimental feature.
// We are actively working on finalizing the semantics of DDL operation.
// As of today,
// the public plan aims to expose as minimum implementation details as possible
// to avoid breaking changes in the feature.
// Similarly, to achieve consistent behavior, we gate-keep some of the features that are in early stage of development
internal object DDLFeatureGate {

    fun gate(node: Statement.DDL) {
        Visitor.visitStatementDDL(node, Unit)
    }

    internal object Visitor : PlanBaseVisitor<Unit, Unit>() {
        override fun defaultReturn(node: PlanNode, ctx: Unit) = Unit

        override fun visitTypeCollection(node: Type.Collection, ctx: Unit) {
            when (val type = node.type) {
                is Type.Collection -> {
                    val elementType = type.type ?: return super.visitTypeCollection(node, ctx)
                    if (elementType is Type.Collection) {
                        if (elementType.constraints.isNotEmpty()) {
                            TODO("Unsupported Feature - nested Collection Constraint")
                        } else super.visitTypeCollection(node, ctx)
                    }
                }
                else -> super.visitTypeCollection(node, ctx)
            }
        }

        override fun visitTypeRecordField(node: Type.Record.Field, ctx: Unit) {
            val fieldType = node.type
            if (fieldType is Type.Record) {
                if (fieldType.constraints.isNotEmpty()) {
                    TODO("Unsupported Feature - Check constraint on Struct Field")
                } else super.visitTypeRecordField(node, ctx)
            } else super.visitTypeRecordField(node, ctx)
        }

        override fun visitConstraint(node: Constraint, ctx: Unit) {
            val name = node.name ?: return
            if (!name.startsWith("$"))
                TODO("Unsupported Feature - Named constraint")
        }
    }
}
