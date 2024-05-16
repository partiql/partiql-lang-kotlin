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
        Visitor.visitStatementDDL(node, Visitor.Ctx(0))
    }

    internal object Visitor : PlanBaseVisitor<Unit, Visitor.Ctx>() {
        // For blocking nested collection
        data class Ctx(
            val level: Int
        )
        override fun defaultReturn(node: PlanNode, ctx: Ctx) = Unit

        override fun visitTypeCollection(node: Type.Collection, ctx: Ctx) {
            if (!node.isOrdered && ctx.level != 0) TODO("UNSUPPORTED Features, using Bag type as attribute type is not supported yet")
            if (node.isOrdered && ctx.level != 1) TODO("UNSUPPORTED Features, using the collection type as element of collection type is not supported yet")
            val nextLevel = Ctx(ctx.level + 1)
            when (val collectionElementType = node.type) {
                is Type.Collection -> {
                    val elementType = collectionElementType.type ?: return super.visitTypeCollection(node, nextLevel)
                    if (elementType is Type.Collection) {
                        if (elementType.constraints.isNotEmpty()) {
                            TODO("Unsupported Feature - nested Collection Constraint")
                        }
                    }
                    super.visitTypeCollection(node, nextLevel)
                }
                else -> super.visitTypeCollection(node, nextLevel)
            }
        }

        override fun visitTypeRecordField(node: Type.Record.Field, ctx: Ctx) {
            val fieldType = node.type
            if (fieldType is Type.Record) {
                if (fieldType.constraints.isNotEmpty()) {
                    TODO("Unsupported Feature - Check constraint on Struct Field")
                }
            }
            super.visitTypeRecordField(node, ctx)
        }

        override fun visitConstraint(node: Constraint, ctx: Ctx) {
            val name = node.name ?: return
            if (!name.startsWith("$"))
                TODO("Unsupported Feature - Named constraint")
        }
    }
}
