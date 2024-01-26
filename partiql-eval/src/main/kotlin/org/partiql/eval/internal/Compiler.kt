package org.partiql.eval.internal

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.internal.operator.rel.RelDistinct
import org.partiql.eval.internal.operator.rel.RelFilter
import org.partiql.eval.internal.operator.rel.RelJoinInner
import org.partiql.eval.internal.operator.rel.RelJoinLeft
import org.partiql.eval.internal.operator.rel.RelJoinOuterFull
import org.partiql.eval.internal.operator.rel.RelJoinRight
import org.partiql.eval.internal.operator.rel.RelProject
import org.partiql.eval.internal.operator.rel.RelScan
import org.partiql.eval.internal.operator.rel.RelScanIndexed
import org.partiql.eval.internal.operator.rel.RelScanIndexedPermissive
import org.partiql.eval.internal.operator.rel.RelScanPermissive
import org.partiql.eval.internal.operator.rex.ExprCallDynamic
import org.partiql.eval.internal.operator.rex.ExprCallStatic
import org.partiql.eval.internal.operator.rex.ExprCase
import org.partiql.eval.internal.operator.rex.ExprCollection
import org.partiql.eval.internal.operator.rex.ExprGlobal
import org.partiql.eval.internal.operator.rex.ExprLiteral
import org.partiql.eval.internal.operator.rex.ExprPathIndex
import org.partiql.eval.internal.operator.rex.ExprPathKey
import org.partiql.eval.internal.operator.rex.ExprPathSymbol
import org.partiql.eval.internal.operator.rex.ExprPermissive
import org.partiql.eval.internal.operator.rex.ExprPivot
import org.partiql.eval.internal.operator.rex.ExprPivotPermissive
import org.partiql.eval.internal.operator.rex.ExprSelect
import org.partiql.eval.internal.operator.rex.ExprStruct
import org.partiql.eval.internal.operator.rex.ExprTupleUnion
import org.partiql.eval.internal.operator.rex.ExprVar
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.debug.PlanPrinter
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import java.lang.IllegalStateException

internal class Compiler @OptIn(PartiQLFunctionExperimental::class) constructor(
    private val plan: PartiQLPlan,
    private val session: PartiQLEngine.Session
) : PlanBaseVisitor<Operator, Unit>() {

    fun compile(): Operator.Expr {
        return visitPartiQLPlan(plan, Unit)
    }

    override fun defaultReturn(node: PlanNode, ctx: Unit): Operator {
        TODO("Not yet implemented")
    }

    override fun visitRexOpErr(node: Rex.Op.Err, ctx: Unit): Operator {
        val message = buildString {
            this.appendLine(node.message)
            PlanPrinter.append(this, plan)
        }
        throw IllegalStateException(message)
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
        return visitRex(node.root, ctx).modeHandled()
    }

    // REX

    override fun visitRex(node: Rex, ctx: Unit): Operator.Expr {
        return super.visitRexOp(node.op, ctx) as Operator.Expr
    }

    override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: Unit): Operator {
        val values = node.values.map { visitRex(it, ctx).modeHandled() }
        return ExprCollection(values)
    }
    override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: Unit): Operator {
        val fields = node.fields.map {
            val value = visitRex(it.v, ctx).modeHandled()
            ExprStruct.Field(visitRex(it.k, ctx), value)
        }
        return ExprStruct(fields)
    }

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: Unit): Operator {
        val rel = visitRel(node.rel, ctx)
        val constructor = visitRex(node.constructor, ctx).modeHandled()
        return ExprSelect(rel, constructor)
    }

    override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: Unit): Operator {
        val rel = visitRel(node.rel, ctx)
        val key = visitRex(node.key, ctx)
        val value = visitRex(node.value, ctx)
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> ExprPivotPermissive(rel, key, value)
            PartiQLEngine.Mode.STRICT -> ExprPivot(rel, key, value)
        }
    }
    override fun visitRexOpVar(node: Rex.Op.Var, ctx: Unit): Operator {
        return ExprVar(node.ref)
    }

    override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: Unit): Operator {
        val catalog = plan.catalogs[node.ref.catalog]
        val symbol = catalog.symbols[node.ref.symbol]
        val path = ConnectorObjectPath(symbol.path)
        val bindings = session.bindings[catalog.name]!!
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

    @OptIn(PartiQLFunctionExperimental::class, PartiQLValueExperimental::class)
    override fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: Unit): Operator {
        val function = getFunction(node.fn.signature)
        val args = node.args.map { visitRex(it, ctx) }.toTypedArray()
        val fnTakesInMissing = function.signature.parameters.any {
            it.type == PartiQLValueType.MISSING || it.type == PartiQLValueType.ANY
        }
        return when (fnTakesInMissing) {
            true -> ExprCallStatic(function, args.map { it.modeHandled() }.toTypedArray())
            false -> ExprCallStatic(function, args)
        }
    }

    @OptIn(PartiQLFunctionExperimental::class, PartiQLValueExperimental::class)
    override fun visitRexOpCallDynamic(node: Rex.Op.Call.Dynamic, ctx: Unit): Operator {
        val args = node.args.map { visitRex(it, ctx).modeHandled() }.toTypedArray()
        val candidates = node.candidates.map { candidate ->
            val fn = getFunction(candidate.fn.signature)
            val coercions = candidate.coercions.map { it?.signature?.let { sig -> getFunction(sig) } }
            ExprCallDynamic.Candidate(candidate.parameters.toTypedArray(), fn, coercions)
        }
        return ExprCallDynamic(candidates, args)
    }

    @OptIn(PartiQLFunctionExperimental::class)
    private fun getFunction(signature: FunctionSignature): PartiQLFunction.Scalar {
        // TODO: .flatMap is a HACK. Once functions in the plan reference functions in a catalog, we will need to
        //  query that connector. This should be a somewhat simple change.
        val matches = session.functions
            .flatMap { it.value }
            .filterIsInstance<PartiQLFunction.Scalar>()
            .filter { it.signature == signature }

        return when (matches.size) {
            0 -> error("No matches encountered for $signature")
            1 -> matches.first()
            else -> error("Multiple matches encountered for $signature")
        }
    }

    // REL
    override fun visitRel(node: Rel, ctx: Unit): Operator.Relation {
        return super.visitRelOp(node.op, ctx) as Operator.Relation
    }

    override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Unit): Operator {
        val rex = visitRex(node.rex, ctx)
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> RelScanPermissive(rex)
            PartiQLEngine.Mode.STRICT -> RelScan(rex)
        }
    }

    override fun visitRelOpProject(node: Rel.Op.Project, ctx: Unit): Operator {
        val input = visitRel(node.input, ctx)
        val projections = node.projections.map { visitRex(it, ctx).modeHandled() }
        return RelProject(input, projections)
    }

    override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: Unit): Operator {
        val rex = visitRex(node.rex, ctx)
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> RelScanIndexedPermissive(rex)
            PartiQLEngine.Mode.STRICT -> RelScanIndexed(rex)
        }
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

    override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: Unit): Operator {
        val input = visitRel(node.input, ctx)
        return RelDistinct(input)
    }

    override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Unit): Operator {
        val input = visitRel(node.input, ctx)
        val condition = visitRex(node.predicate, ctx)
        return RelFilter(input, condition)
    }

    // HELPERS
    private fun Operator.Expr.modeHandled(): Operator.Expr {
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> ExprPermissive(this)
            PartiQLEngine.Mode.STRICT -> this
        }
    }
}
