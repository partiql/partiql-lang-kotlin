package org.partiql.eval.internal

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.internal.operator.rel.RelAggregate
import org.partiql.eval.internal.operator.rel.RelDistinct
import org.partiql.eval.internal.operator.rel.RelExceptAll
import org.partiql.eval.internal.operator.rel.RelExceptDistinct
import org.partiql.eval.internal.operator.rel.RelExclude
import org.partiql.eval.internal.operator.rel.RelFilter
import org.partiql.eval.internal.operator.rel.RelIntersectAll
import org.partiql.eval.internal.operator.rel.RelIntersectDistinct
import org.partiql.eval.internal.operator.rel.RelJoinInner
import org.partiql.eval.internal.operator.rel.RelJoinOuterFull
import org.partiql.eval.internal.operator.rel.RelJoinOuterLeft
import org.partiql.eval.internal.operator.rel.RelJoinOuterRight
import org.partiql.eval.internal.operator.rel.RelLimit
import org.partiql.eval.internal.operator.rel.RelOffset
import org.partiql.eval.internal.operator.rel.RelProject
import org.partiql.eval.internal.operator.rel.RelScan
import org.partiql.eval.internal.operator.rel.RelScanIndexed
import org.partiql.eval.internal.operator.rel.RelScanIndexedPermissive
import org.partiql.eval.internal.operator.rel.RelScanPermissive
import org.partiql.eval.internal.operator.rel.RelSort
import org.partiql.eval.internal.operator.rel.RelUnionAll
import org.partiql.eval.internal.operator.rel.RelUnionDistinct
import org.partiql.eval.internal.operator.rel.RelUnpivot
import org.partiql.eval.internal.operator.rex.ExprArithmeticBinary
import org.partiql.eval.internal.operator.rex.ExprArithmeticUnary
import org.partiql.eval.internal.operator.rex.ExprCallDynamic
import org.partiql.eval.internal.operator.rex.ExprCallStatic
import org.partiql.eval.internal.operator.rex.ExprCase
import org.partiql.eval.internal.operator.rex.ExprCast
import org.partiql.eval.internal.operator.rex.ExprCoalesce
import org.partiql.eval.internal.operator.rex.ExprCollection
import org.partiql.eval.internal.operator.rex.ExprLiteral
import org.partiql.eval.internal.operator.rex.ExprMissing
import org.partiql.eval.internal.operator.rex.ExprNullIf
import org.partiql.eval.internal.operator.rex.ExprPathIndex
import org.partiql.eval.internal.operator.rex.ExprPathKey
import org.partiql.eval.internal.operator.rex.ExprPathSymbol
import org.partiql.eval.internal.operator.rex.ExprPermissive
import org.partiql.eval.internal.operator.rex.ExprPivot
import org.partiql.eval.internal.operator.rex.ExprPivotPermissive
import org.partiql.eval.internal.operator.rex.ExprSelect
import org.partiql.eval.internal.operator.rex.ExprStruct
import org.partiql.eval.internal.operator.rex.ExprSubquery
import org.partiql.eval.internal.operator.rex.ExprTupleUnion
import org.partiql.eval.internal.operator.rex.ExprVarLocal
import org.partiql.eval.internal.operator.rex.ExprVarOuter
import org.partiql.eval.value.Datum
import org.partiql.plan.Catalog
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Ref
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.debug.PlanPrinter
import org.partiql.plan.rexOpErr
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.FnExperimental
import org.partiql.types.PType
import org.partiql.value.PartiQLValueExperimental
import java.lang.IllegalStateException

internal class Compiler(
    private val plan: PartiQLPlan,
    private val session: PartiQLEngine.Session,
    private val symbols: Symbols,
) : PlanBaseVisitor<Operator, PType?>() {

    fun compile(): Operator.Expr {
        return visitPartiQLPlan(plan, null)
    }

    override fun defaultReturn(node: PlanNode, ctx: PType?): Operator {
        TODO("Not yet implemented")
    }

    override fun visitRexOpErr(node: Rex.Op.Err, ctx: PType?): Operator {
        val message = buildString {
            this.appendLine(node.message)
            PlanPrinter.append(this, plan)
        }
        throw IllegalStateException(message)
    }

    override fun visitRelOpErr(node: Rel.Op.Err, ctx: PType?): Operator {
        throw IllegalStateException(node.message)
    }

    override fun visitPartiQLPlan(node: PartiQLPlan, ctx: PType?): Operator.Expr {
        return visitStatement(node.statement, ctx) as Operator.Expr
    }

    override fun visitStatementQuery(node: Statement.Query, ctx: PType?): Operator.Expr {
        return visitRex(node.root, ctx).modeHandled()
    }

    // REX

    override fun visitRex(node: Rex, ctx: PType?): Operator.Expr {
        return super.visitRexOp(node.op, node.type) as Operator.Expr
    }

    override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: PType?): Operator {
        val values = node.values.map { visitRex(it, ctx).modeHandled() }
        val type = ctx ?: error("No type provided in ctx")
        return ExprCollection(values, type)
    }

    override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: PType?): Operator {
        val fields = node.fields.map {
            val value = visitRex(it.v, ctx).modeHandled()
            ExprStruct.Field(visitRex(it.k, ctx), value)
        }
        return ExprStruct(fields)
    }

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: PType?): Operator {
        val rel = visitRel(node.rel, ctx)
        val ordered = node.rel.type.props.contains(Rel.Prop.ORDERED)
        val constructor = visitRex(node.constructor, ctx).modeHandled()
        return ExprSelect(rel, constructor, ordered)
    }

    override fun visitRexOpSubquery(node: Rex.Op.Subquery, ctx: PType?): Operator {
        val constructor = visitRex(node.constructor, ctx)
        val input = visitRel(node.rel, ctx)
        return when (node.coercion) {
            Rex.Op.Subquery.Coercion.SCALAR -> ExprSubquery.Scalar(constructor, input)
            Rex.Op.Subquery.Coercion.ROW -> ExprSubquery.Row(constructor, input)
        }
    }

    override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: PType?): Operator {
        val rel = visitRel(node.rel, ctx)
        val key = visitRex(node.key, ctx)
        val value = visitRex(node.value, ctx)
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> ExprPivotPermissive(rel, key, value)
            PartiQLEngine.Mode.STRICT -> ExprPivot(rel, key, value)
        }
    }

    override fun visitRexOpCoalesce(node: Rex.Op.Coalesce, ctx: PType?): Operator {
        val args = Array(node.args.size) { visitRex(node.args[it], node.args[it].type) }
        return ExprCoalesce(args)
    }

    override fun visitRexOpNullif(node: Rex.Op.Nullif, ctx: PType?): Operator {
        val value = visitRex(node.value, node.value.type)
        val nullifier = visitRex(node.nullifier, node.value.type)
        return ExprNullIf(value, nullifier)
    }

    /**
     * All variables from the local scope have a depth of 0.
     *
     * All variables coming from the stack have a depth > 0. To slightly minimize computation at execution, we subtract
     * the depth by 1 to account for the fact that the local scope is not kept on the stack.
     */
    override fun visitRexOpVar(node: Rex.Op.Var, ctx: PType?): Operator {
        return when (node.depth) {
            0 -> ExprVarLocal(node.ref)
            else -> {
                ExprVarOuter(node.depth, node.ref)
            }
        }
    }

    override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: PType?): Operator = symbols.getGlobal(node.ref)

    override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: PType?): Operator.Relation {
        val input = visitRel(node.input, ctx)
        val calls = node.calls.map {
            visitRelOpAggregateCall(it, ctx)
        }
        val groups = node.groups.map { visitRex(it, ctx).modeHandled() }
        return RelAggregate(input, groups, calls)
    }

    @OptIn(FnExperimental::class)
    override fun visitRelOpAggregateCall(node: Rel.Op.Aggregate.Call, ctx: PType?): Operator.Aggregation {
        val args = node.args.map { visitRex(it, it.type).modeHandled() }
        val setQuantifier: Operator.Aggregation.SetQuantifier = when (node.setQuantifier) {
            Rel.Op.Aggregate.Call.SetQuantifier.ALL -> Operator.Aggregation.SetQuantifier.ALL
            Rel.Op.Aggregate.Call.SetQuantifier.DISTINCT -> Operator.Aggregation.SetQuantifier.DISTINCT
        }
        val agg = symbols.getAgg(node.agg)
        return object : Operator.Aggregation {
            override val delegate: Agg = agg
            override val args: List<Operator.Expr> = args
            override val setQuantifier: Operator.Aggregation.SetQuantifier = setQuantifier
        }
    }

    override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: PType?): Operator {
        val root = visitRex(node.root, ctx)
        val key = visitRex(node.key, ctx)
        return ExprPathKey(root, key)
    }

    override fun visitRexOpPathSymbol(node: Rex.Op.Path.Symbol, ctx: PType?): Operator {
        val root = visitRex(node.root, ctx)
        val symbol = node.key
        return ExprPathSymbol(root, symbol)
    }

    override fun visitRexOpPathIndex(node: Rex.Op.Path.Index, ctx: PType?): Operator {
        val root = visitRex(node.root, ctx)
        val index = visitRex(node.key, ctx)
        return ExprPathIndex(root, index)
    }

    @OptIn(FnExperimental::class)
    override fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: PType?): Operator {
        val fn = symbols.getFn(node.fn)
        val args = node.args.map { visitRex(it, ctx) }.toTypedArray()
        val fnTakesInMissing = fn.signature.parameters.any {
            it.type.kind == PType.Kind.DYNAMIC // TODO: Is this needed?
        }
        return when (fnTakesInMissing) {
            true -> ExprCallStatic(fn, args.map { it.modeHandled() }.toTypedArray())
            false -> ExprCallStatic(fn, args)
        }
    }

    @OptIn(FnExperimental::class)
    override fun visitRexOpCallDynamic(node: Rex.Op.Call.Dynamic, ctx: PType?): Operator {
        val args = node.args.map { visitRex(it, ctx).modeHandled() }.toTypedArray()
        // Check candidate list size
        when (node.candidates.size) {
            0 -> error("Rex.Op.Call.Dynamic had an empty candidates list: $node.")
            // TODO this seems like it should be an error, but is possible if the fn match was non-exhaustive
            // 1 -> error("Rex.Op.Call.Dynamic had a single candidate; should be Rex.Op.Call.Static")
        }
        // Check candidate name and arity for uniformity
        var arity: Int = -1
        var name: String = "unknown"
        // Compile the candidates
        val candidates = Array(node.candidates.size) {
            val candidate = node.candidates[it]
            val fn = symbols.getFn(candidate.fn)
            val coercions = candidate.coercions.toTypedArray()
            // Check this candidate
            val fnArity = fn.signature.parameters.size
            val fnName = fn.signature.name.uppercase()
            if (arity == -1) {
                arity = fnArity
                name = fnName
            } else {
                if (fnArity != arity) {
                    error("Dynamic call candidate had different arity than others; found $fnArity but expected $arity")
                }
                if (fnName != name) {
                    error("Dynamic call candidate had different name than others; found $fnName but expected $name")
                }
            }
            ExprCallDynamic.Candidate(fn, coercions)
        }
        return ExprCallDynamic(name, candidates, args)
    }

    override fun visitRexOpCast(node: Rex.Op.Cast, ctx: PType?): Operator {
        return ExprCast(visitRex(node.arg, ctx), node.cast)
    }

    override fun visitRexOpMissing(node: Rex.Op.Missing, ctx: PType?): Operator {
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> {
                // Make a runtime TypeCheckException.
                ExprMissing(node.message)
            }
            PartiQLEngine.Mode.STRICT -> {
                // Promote to error.
                visitRexOpErr(rexOpErr(node.message, node.causes), null)
            }
        }
    }

    override fun visitRexOpAdd(node: Rex.Op.Add, ctx: PType?): Operator {
        val (lhs, rhs) = visitBinaryArithmeticArgs(node.lhs, node.rhs, ctx!!)
        val factory = getArithmeticBinaryFactory(ctx)
        return factory.add(lhs, rhs)
    }

    override fun visitRexOpMultiply(node: Rex.Op.Multiply, ctx: PType?): Operator {
        val (lhs, rhs) = visitBinaryArithmeticArgs(node.lhs, node.rhs, ctx!!)
        val factory = getArithmeticBinaryFactory(ctx)
        return factory.multiply(lhs, rhs)
    }

    override fun visitRexOpDivide(node: Rex.Op.Divide, ctx: PType?): Operator {
        val (lhs, rhs) = visitBinaryArithmeticArgs(node.lhs, node.rhs, ctx!!)
        val factory = getArithmeticBinaryFactory(ctx)
        return factory.divide(lhs, rhs)
    }

    override fun visitRexOpSubtract(node: Rex.Op.Subtract, ctx: PType?): Operator {
        val (lhs, rhs) = visitBinaryArithmeticArgs(node.lhs, node.rhs, ctx!!)
        val factory = getArithmeticBinaryFactory(ctx)
        return factory.subtract(lhs, rhs)
    }

    override fun visitRexOpModulo(node: Rex.Op.Modulo, ctx: PType?): Operator {
        val (lhs, rhs) = visitBinaryArithmeticArgs(node.lhs, node.rhs, ctx!!)
        val factory = getArithmeticBinaryFactory(ctx)
        return factory.modulo(lhs, rhs)
    }

    override fun visitRexOpNegative(node: Rex.Op.Negative, ctx: PType?): Operator {
        val arg = visitRex(node.arg, ctx)
        val factory = getArithmeticUnaryFactory(node.arg.type)
        return factory.negative(arg)
    }

    override fun visitRexOpPositive(node: Rex.Op.Positive, ctx: PType?): Operator {
        val arg = visitRex(node.arg, ctx)
        val factory = getArithmeticUnaryFactory(node.arg.type)
        return factory.positive(arg)
    }

    private fun visitBinaryArithmeticArgs(l: Rex, r: Rex, returnType: PType): Pair<Operator.Expr, Operator.Expr> {
        val lhsVisited = visitRex(l, l.type)
        val rhsVisited = visitRex(r, r.type)
        return when (returnType.kind) {
            PType.Kind.TINYINT, PType.Kind.SMALLINT, PType.Kind.INT, PType.Kind.BIGINT, PType.Kind.INT_ARBITRARY,
            PType.Kind.REAL, PType.Kind.DOUBLE_PRECISION,
            PType.Kind.DECIMAL_ARBITRARY -> lhsVisited.coerce(l.type, returnType) to rhsVisited.coerce(r.type, returnType)
            PType.Kind.DECIMAL -> lhsVisited.coerce(l.type, returnType) to rhsVisited.coerce(r.type, PType.typeDecimalArbitrary())
            PType.Kind.DYNAMIC -> lhsVisited to rhsVisited
            else -> error("Unsupported type: $returnType")
        }
    }

    private fun getArithmeticUnaryFactory(returns: PType): ExprArithmeticUnary.Factory {
        return when (returns.kind) {
            PType.Kind.TINYINT -> ExprArithmeticUnary.Factory.Byte
            PType.Kind.SMALLINT -> ExprArithmeticUnary.Factory.Short
            PType.Kind.INT -> ExprArithmeticUnary.Factory.Int
            PType.Kind.BIGINT -> ExprArithmeticUnary.Factory.BigInt
            PType.Kind.INT_ARBITRARY -> ExprArithmeticUnary.Factory.IntArbitrary
            PType.Kind.REAL -> ExprArithmeticUnary.Factory.Float
            PType.Kind.DOUBLE_PRECISION -> ExprArithmeticUnary.Factory.Double
            PType.Kind.DECIMAL -> ExprArithmeticUnary.Factory.Decimal(returns)
            PType.Kind.DECIMAL_ARBITRARY -> ExprArithmeticUnary.Factory.DecimalArbitrary
            PType.Kind.DYNAMIC -> ExprArithmeticUnary.Factory.Dynamic
            else -> error("Unsupported type: $returns")
        }
    }

    private fun getArithmeticBinaryFactory(returns: PType): ExprArithmeticBinary.Factory {
        return when (returns.kind) {
            PType.Kind.TINYINT -> ExprArithmeticBinary.Factory.Byte
            PType.Kind.SMALLINT -> ExprArithmeticBinary.Factory.Short
            PType.Kind.INT -> ExprArithmeticBinary.Factory.Int
            PType.Kind.BIGINT -> ExprArithmeticBinary.Factory.BigInt
            PType.Kind.INT_ARBITRARY -> ExprArithmeticBinary.Factory.IntArbitrary
            PType.Kind.REAL -> ExprArithmeticBinary.Factory.Float
            PType.Kind.DOUBLE_PRECISION -> ExprArithmeticBinary.Factory.Double
            PType.Kind.DECIMAL -> ExprArithmeticBinary.Factory.Decimal(returns)
            PType.Kind.DECIMAL_ARBITRARY -> ExprArithmeticBinary.Factory.DecimalArbitrary
            PType.Kind.DYNAMIC -> ExprArithmeticBinary.Factory.Dynamic
            else -> error("Unsupported type: $returns")
        }
    }

    private fun Operator.Expr.coerce(input: PType, target: PType): Operator.Expr {
        if (input == target) return this
        return ExprCast(this, Ref.Cast(input, target, isNullable = true))
    }

    // REL
    override fun visitRel(node: Rel, ctx: PType?): Operator.Relation {
        return super.visitRelOp(node.op, ctx) as Operator.Relation
    }

    override fun visitRelOpScan(node: Rel.Op.Scan, ctx: PType?): Operator {
        val rex = visitRex(node.rex, ctx)
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> RelScanPermissive(rex)
            PartiQLEngine.Mode.STRICT -> RelScan(rex)
        }
    }

    override fun visitRelOpProject(node: Rel.Op.Project, ctx: PType?): Operator {
        val input = visitRel(node.input, ctx)
        val projections = node.projections.map { visitRex(it, ctx).modeHandled() }
        return RelProject(input, projections)
    }

    override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: PType?): Operator {
        val rex = visitRex(node.rex, ctx)
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> RelScanIndexedPermissive(rex)
            PartiQLEngine.Mode.STRICT -> RelScanIndexed(rex)
        }
    }

    override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: PType?): Operator {
        val expr = visitRex(node.rex, ctx)
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> RelUnpivot.Permissive(expr)
            PartiQLEngine.Mode.STRICT -> RelUnpivot.Strict(expr)
        }
    }

    override fun visitRelOpSetExcept(node: Rel.Op.Set.Except, ctx: PType?): Operator {
        val lhs = visitRel(node.lhs, ctx)
        val rhs = visitRel(node.rhs, ctx)
        return when (node.quantifier) {
            Rel.Op.Set.Quantifier.ALL -> RelExceptAll(lhs, rhs)
            Rel.Op.Set.Quantifier.DISTINCT -> RelExceptDistinct(lhs, rhs)
        }
    }

    override fun visitRelOpSetIntersect(node: Rel.Op.Set.Intersect, ctx: PType?): Operator {
        val lhs = visitRel(node.lhs, ctx)
        val rhs = visitRel(node.rhs, ctx)
        return when (node.quantifier) {
            Rel.Op.Set.Quantifier.ALL -> RelIntersectAll(lhs, rhs)
            Rel.Op.Set.Quantifier.DISTINCT -> RelIntersectDistinct(lhs, rhs)
        }
    }

    override fun visitRelOpSetUnion(node: Rel.Op.Set.Union, ctx: PType?): Operator {
        val lhs = visitRel(node.lhs, ctx)
        val rhs = visitRel(node.rhs, ctx)
        return when (node.quantifier) {
            Rel.Op.Set.Quantifier.ALL -> RelUnionAll(lhs, rhs)
            Rel.Op.Set.Quantifier.DISTINCT -> RelUnionDistinct(lhs, rhs)
        }
    }

    override fun visitRelOpLimit(node: Rel.Op.Limit, ctx: PType?): Operator {
        val input = visitRel(node.input, ctx)
        val limit = visitRex(node.limit, ctx)
        return RelLimit(input, limit)
    }

    override fun visitRelOpOffset(node: Rel.Op.Offset, ctx: PType?): Operator {
        val input = visitRel(node.input, ctx)
        val offset = visitRex(node.offset, ctx)
        return RelOffset(input, offset)
    }

    override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: PType?): Operator {
        val args = node.args.map { visitRex(it, ctx) }.toTypedArray()
        return ExprTupleUnion(args)
    }

    override fun visitRelOpJoin(node: Rel.Op.Join, ctx: PType?): Operator {
        val lhs = visitRel(node.lhs, ctx)
        val rhs = visitRel(node.rhs, ctx)
        val condition = visitRex(node.rex, ctx)
        return when (node.type) {
            Rel.Op.Join.Type.INNER -> RelJoinInner(lhs, rhs, condition)
            Rel.Op.Join.Type.LEFT -> RelJoinOuterLeft(lhs, rhs, condition, rhsType = node.rhs.type)
            Rel.Op.Join.Type.RIGHT -> RelJoinOuterRight(lhs, rhs, condition, lhsType = node.lhs.type)
            Rel.Op.Join.Type.FULL -> RelJoinOuterFull(lhs, rhs, condition, lhsType = node.lhs.type, rhsType = node.rhs.type)
        }
    }

    override fun visitRexOpCase(node: Rex.Op.Case, ctx: PType?): Operator {
        val branches = node.branches.map { branch ->
            visitRex(branch.condition, ctx).modeHandled() to visitRex(branch.rex, ctx)
        }
        val default = visitRex(node.default, ctx)
        return ExprCase(branches, default)
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun visitRexOpLit(node: Rex.Op.Lit, ctx: PType?): Operator {
        return ExprLiteral(Datum.of(node.value))
    }

    override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: PType?): Operator {
        val input = visitRel(node.input, ctx)
        return RelDistinct(input)
    }

    override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: PType?): Operator {
        val input = visitRel(node.input, ctx)
        val condition = visitRex(node.predicate, ctx).modeHandled()
        return RelFilter(input, condition)
    }

    override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: PType?): Operator {
        val input = visitRel(node.input, ctx)
        return RelExclude(input, node.paths)
    }

    override fun visitRelOpSort(node: Rel.Op.Sort, ctx: PType?): Operator {
        val input = visitRel(node.input, ctx)
        val compiledSpecs = node.specs.map { spec ->
            val expr = visitRex(spec.rex, ctx)
            val order = spec.order
            expr to order
        }
        return RelSort(input, compiledSpecs)
    }

    // HELPERS

    private fun Operator.Expr.modeHandled(): Operator.Expr {
        return when (session.mode) {
            PartiQLEngine.Mode.PERMISSIVE -> ExprPermissive(this)
            PartiQLEngine.Mode.STRICT -> this
        }
    }

    /**
     * Get a typed catalog item from a reference
     *
     * @param T
     * @return
     */
    private inline fun <reified T : Catalog.Item> Ref.get(): T {
        val item = plan.catalogs.getOrNull(catalog)?.items?.get(symbol)
        if (item == null || item !is T) {
            error("Invalid catalog reference, $this for type ${T::class}")
        }
        return item
    }

    internal companion object {
        private fun Operator.Expr.coerce(input: PType, target: PType): Operator.Expr {
            if (input == target) return this
            return ExprCast(this, Ref.Cast(input, target, isNullable = true))
        }
    }
}
