package org.partiql.eval.impl

import org.partiql.eval.impl.expression.Collection
import org.partiql.eval.impl.expression.Literal
import org.partiql.eval.impl.expression.Select
import org.partiql.eval.impl.expression.Struct
import org.partiql.eval.impl.expression.Variable
import org.partiql.eval.impl.relation.Projection
import org.partiql.eval.impl.relation.Scan
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.value.PartiQLValueExperimental

internal object PlanToPhysical {

    fun convert(plan: PartiQLPlan): PhysicalNode.Expression {
        return PlanToCodeTransformer.visitPartiQLPlan(plan, Unit)
    }

    private object PlanToCodeTransformer : PlanBaseVisitor<PhysicalNode, Unit>() {
        override fun defaultReturn(node: PlanNode, ctx: Unit): PhysicalNode {
            TODO("Not yet implemented")
        }

        override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: Unit): PhysicalNode {
            val fields = node.fields.map {
                Struct.Field(visitRex(it.k, ctx), visitRex(it.v, ctx))
            }
            return Struct(fields)
        }

        override fun visitRexOpVar(node: Rex.Op.Var, ctx: Unit): PhysicalNode {
            return Variable(node.ref)
        }

        override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: Unit): PhysicalNode {
            val values = node.values.map { visitRex(it, ctx) }
            return Collection(values)
        }

        override fun visitRelOpProject(node: Rel.Op.Project, ctx: Unit): PhysicalNode {
            val input = visitRel(node.input, ctx)
            val projections = node.projections.map { visitRex(it, ctx) }
            return Projection(input, projections)
        }

        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: Unit): PhysicalNode {
            val rel = visitRel(node.rel, ctx)
            val constructor = visitRex(node.constructor, ctx)
            return Select(rel, constructor)
        }

        override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Unit): PhysicalNode {
            val rex = visitRex(node.rex, ctx)
            return Scan(rex)
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpLit(node: Rex.Op.Lit, ctx: Unit): PhysicalNode {
            return Literal(node.value)
        }

        override fun visitRel(node: Rel, ctx: Unit): PhysicalNode.Relation {
            return super.visitRelOp(node.op, ctx) as PhysicalNode.Relation
        }

        override fun visitRex(node: Rex, ctx: Unit): PhysicalNode.Expression {
            return super.visitRexOp(node.op, ctx) as PhysicalNode.Expression
        }

        // TODO: Re-look at
        override fun visitPartiQLPlan(node: PartiQLPlan, ctx: Unit): PhysicalNode.Expression {
            return visitStatement(node.statement, ctx) as PhysicalNode.Expression
        }

        // TODO: Re-look at
        override fun visitStatementQuery(node: Statement.Query, ctx: Unit): PhysicalNode.Expression {
            return visitRex(node.root, ctx)
        }
    }
}
