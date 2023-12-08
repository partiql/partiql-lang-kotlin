package org.partiql.eval.internal

import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.internal.operator.rex.ExprCollection
import org.partiql.eval.internal.operator.rex.ExprLiteral
import org.partiql.eval.internal.operator.rex.ExprSelect
import org.partiql.eval.internal.operator.rex.ExprStruct
import org.partiql.eval.internal.operator.rex.ExprVar
import org.partiql.eval.internal.operator.rel.RelProject
import org.partiql.eval.internal.operator.rel.RelScan
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.value.PartiQLValueExperimental

internal object Compiler {

    fun compile(plan: PartiQLPlan): Operator.Expr {
        return PlanToCodeTransformer.visitPartiQLPlan(plan, Unit)
    }

    private object PlanToCodeTransformer : PlanBaseVisitor<Operator, Unit>() {
        override fun defaultReturn(node: PlanNode, ctx: Unit): Operator {
            TODO("Not yet implemented")
        }

        override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: Unit): Operator {
            val fields = node.fields.map {
                ExprStruct.Field(visitRex(it.k, ctx), visitRex(it.v, ctx))
            }
            return ExprStruct(fields)
        }

        override fun visitRexOpVar(node: Rex.Op.Var, ctx: Unit): Operator {
            return ExprVar(node.ref)
        }

        override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: Unit): Operator {
            val values = node.values.map { visitRex(it, ctx) }
            return ExprCollection(values)
        }

        override fun visitRelOpProject(node: Rel.Op.Project, ctx: Unit): Operator {
            val input = visitRel(node.input, ctx)
            val projections = node.projections.map { visitRex(it, ctx) }
            return RelProject(input, projections)
        }

        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: Unit): Operator {
            val rel = visitRel(node.rel, ctx)
            val constructor = visitRex(node.constructor, ctx)
            return ExprSelect(rel, constructor)
        }

        override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Unit): Operator {
            val rex = visitRex(node.rex, ctx)
            return RelScan(rex)
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpLit(node: Rex.Op.Lit, ctx: Unit): Operator {
            return ExprLiteral(node.value)
        }

        override fun visitRel(node: Rel, ctx: Unit): Operator.Relation {
            return super.visitRelOp(node.op, ctx) as Operator.Relation
        }

        override fun visitRex(node: Rex, ctx: Unit): Operator.Expr {
            return super.visitRexOp(node.op, ctx) as Operator.Expr
        }

        // TODO: Re-look at
        override fun visitPartiQLPlan(node: PartiQLPlan, ctx: Unit): Operator.Expr {
            return visitStatement(node.statement, ctx) as Operator.Expr
        }

        // TODO: Re-look at
        override fun visitStatementQuery(node: Statement.Query, ctx: Unit): Operator.Expr {
            return visitRex(node.root, ctx)
        }
    }
}
