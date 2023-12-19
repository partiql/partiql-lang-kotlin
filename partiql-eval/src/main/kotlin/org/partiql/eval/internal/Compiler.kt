package org.partiql.eval.internal

import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.internal.operator.rel.RelFilter
import org.partiql.eval.internal.operator.rel.RelJoinInner
import org.partiql.eval.internal.operator.rel.RelJoinLeft
import org.partiql.eval.internal.operator.rel.RelJoinOuterFull
import org.partiql.eval.internal.operator.rel.RelJoinRight
import org.partiql.eval.internal.operator.rel.RelProject
import org.partiql.eval.internal.operator.rel.RelScan
import org.partiql.eval.internal.operator.rel.RelScanIndexed
import org.partiql.eval.internal.operator.rex.ExprCase
import org.partiql.eval.internal.operator.rex.ExprCollection
import org.partiql.eval.internal.operator.rex.ExprGlobal
import org.partiql.eval.internal.operator.rex.ExprLiteral
import org.partiql.eval.internal.operator.rex.ExprPivot
import org.partiql.eval.internal.operator.rex.ExprPathIndex
import org.partiql.eval.internal.operator.rex.ExprPathKey
import org.partiql.eval.internal.operator.rex.ExprPathSymbol
import org.partiql.eval.internal.operator.rex.ExprSelect
import org.partiql.eval.internal.operator.rex.ExprStruct
import org.partiql.eval.internal.operator.rex.ExprTupleUnion
import org.partiql.eval.internal.operator.rex.ExprVar
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.value.PartiQLValueExperimental
import java.lang.IllegalStateException

internal class Compiler(
    private val plan: PartiQLPlan,
    private val catalogs: Map<String, ConnectorBindings>,
) : PlanBaseVisitor<Operator, Unit>() {

    fun compile(): Operator.Expr {
        return visitPartiQLPlan(plan, Unit)
    }

    override fun defaultReturn(node: PlanNode, ctx: Unit): Operator {
        TODO("Not yet implemented")
    }

    override fun visitRexOpErr(node: Rex.Op.Err, ctx: Unit): Operator {
        throw IllegalStateException(node.message)
    }

    override fun visitRelOpErr(node: Rel.Op.Err, ctx: Unit): Operator {
        throw IllegalStateException(node.message)
    }

    // TODO: Re-look at
    override fun visitPartiQLPlan(node: PartiQLPlan, ctx: Unit): Operator.Expr {
        return visitStatement(node.statement, ctx) as Operator.Expr
    }

    // TODO: Re-look at
    override fun visitStatementQuery(node: Statement.Query, ctx: Unit): Operator.Expr {
        return visitRex(node.root, ctx)
    }

    // REX

    override fun visitRex(node: Rex, ctx: Unit): Operator.Expr {
        return super.visitRexOp(node.op, ctx) as Operator.Expr
    }

    override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: Unit): Operator {
        val values = node.values.map { visitRex(it, ctx) }
        return ExprCollection(values)
    }
    override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: Unit): Operator {
        val fields = node.fields.map {
            ExprStruct.Field(visitRex(it.k, ctx), visitRex(it.v, ctx))
        }
        return ExprStruct(fields)
    }

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: Unit): Operator {
        val rel = visitRel(node.rel, ctx)
        val constructor = visitRex(node.constructor, ctx)
        return ExprSelect(rel, constructor)
    }

    override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: Unit): Operator {
        val rel = visitRel(node.rel, ctx)
        val key = visitRex(node.key, ctx)
        val value = visitRex(node.value, ctx)
        return ExprPivot(rel, key, value)
    }
    override fun visitRexOpVar(node: Rex.Op.Var, ctx: Unit): Operator {
        return ExprVar(node.ref)
    }

    override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: Unit): Operator {
        val catalog = plan.catalogs[node.ref.catalog]
        val symbol = catalog.symbols[node.ref.symbol]
        val path = ConnectorObjectPath(symbol.path)
        val bindings = catalogs[catalog.name]!!
        return ExprGlobal(path, bindings)
    }

    override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: Unit): Operator {
        val root = visitRex(node.root, ctx)
        val key = visitRex(node.key, ctx)
        return ExprPathKey(root, key)
    }

    override fun visitRexOpPathSymbol(node: Rex.Op.Path.Symbol, ctx: Unit): Operator {
        val root = visitRex(node.root, ctx)
        val symbol = node.key
        return ExprPathSymbol(root, symbol)
    }

    override fun visitRexOpPathIndex(node: Rex.Op.Path.Index, ctx: Unit): Operator {
        val root = visitRex(node.root, ctx)
        val index = visitRex(node.key, ctx)
        return ExprPathIndex(root, index)
    }

    // REL

    override fun visitRel(node: Rel, ctx: Unit): Operator.Relation {
        return super.visitRelOp(node.op, ctx) as Operator.Relation
    }

    override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Unit): Operator {
        val rex = visitRex(node.rex, ctx)
        return RelScan(rex)
    }

    override fun visitRelOpProject(node: Rel.Op.Project, ctx: Unit): Operator {
        val input = visitRel(node.input, ctx)
        val projections = node.projections.map { visitRex(it, ctx) }
        return RelProject(input, projections)
    }

    override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: Unit): Operator {
        val rex = visitRex(node.rex, ctx)
        return RelScanIndexed(rex)
    }

    override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: Unit): Operator {
        val args = node.args.map { visitRex(it, ctx) }.toTypedArray()
        return ExprTupleUnion(args)
    }

    override fun visitRelOpJoin(node: Rel.Op.Join, ctx: Unit): Operator {
        val lhs = visitRel(node.lhs, ctx)
        val rhs = visitRel(node.rhs, ctx)
        val condition = visitRex(node.rex, ctx)
        return when (node.type) {
            Rel.Op.Join.Type.INNER -> RelJoinInner(lhs, rhs, condition)
            Rel.Op.Join.Type.LEFT -> RelJoinLeft(lhs, rhs, condition)
            Rel.Op.Join.Type.RIGHT -> RelJoinRight(lhs, rhs, condition)
            Rel.Op.Join.Type.FULL -> RelJoinOuterFull(lhs, rhs, condition)
        }
    }

    override fun visitRexOpCase(node: Rex.Op.Case, ctx: Unit): Operator {
        val branches = node.branches.map { branch ->
            visitRex(branch.condition, ctx) to visitRex(branch.rex, ctx)
        }
        val default = visitRex(node.default, ctx)
        return ExprCase(branches, default)
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun visitRexOpLit(node: Rex.Op.Lit, ctx: Unit): Operator {
        return ExprLiteral(node.value)
    }

    override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Unit): Operator {
        val input = visitRel(node.input, ctx)
        val condition = visitRex(node.predicate, ctx)
        return RelFilter(input, condition)
    }
}
