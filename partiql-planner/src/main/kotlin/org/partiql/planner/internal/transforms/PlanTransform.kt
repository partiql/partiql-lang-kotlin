package org.partiql.planner.internal.transforms

import org.partiql.plan.Action
import org.partiql.plan.Collation
import org.partiql.plan.Exclusion
import org.partiql.plan.JoinType
import org.partiql.plan.Operators
import org.partiql.plan.Plan
import org.partiql.plan.rel.RelAggregate
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexCase
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexType
import org.partiql.plan.rex.RexVar
import org.partiql.planner.internal.PlannerFlag
import org.partiql.planner.internal.ir.SetQuantifier
import org.partiql.planner.internal.ir.visitor.PlanBaseVisitor
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import org.partiql.value.DecimalValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.planner.internal.ir.PartiQLPlan as IPlan
import org.partiql.planner.internal.ir.PlanNode as INode
import org.partiql.planner.internal.ir.Rel as IRel
import org.partiql.planner.internal.ir.Rex as IRex
import org.partiql.planner.internal.ir.Statement as IStatement

/**
 * This produces a V1 plan from the internal plan IR.
 *
 * TODO types and schemas!
 */
internal class PlanTransform(private val flags: Set<PlannerFlag>) {

    /**
     * Transform the internal IR to the public plan interfaces.
     *
     * @param internal
     * @param listener
     * @return
     */
    fun transform(internal: IPlan, listener: PErrorListener): Plan {
        val signal = flags.contains(PlannerFlag.SIGNAL_MODE)
        val query = (internal.statement as IStatement.Query)
        val visitor = Visitor(listener, signal)
        val root = visitor.visitRex(query.root, query.root.type)
        val action = Action.Query { root }
        // TODO replace with standard implementations (or just remove plan transform altogether when possible).
        return object : Plan {
            override fun getActions(): MutableList<Action> = mutableListOf(action)
        }
    }

    private class Visitor(
        private val listener: PErrorListener,
        private val signal: Boolean,
    ) : PlanBaseVisitor<Any?, PType>() {

        private val operators = Operators.STANDARD

        override fun defaultReturn(node: INode, ctx: PType): Any {
            TODO("Translation not supported for ${node::class.simpleName}")
        }

        // ERRORS

        /**
         * TODO the following comment comes from the existing implementation, but how does it apply to CAST ??
         *
         * See PartiQL Specification [Section 4.1.1](https://partiql.org/partiql-lang/#sec:schema-in-tuple-path).
         * While it talks about pathing into a tuple, it provides guidance on expressions that always return missing:
         *
         * > In a more important and common case, an PartiQL implementation can utilize the input data schema to prove
         * > that a path expression always returns MISSING and thus throw a compile-time error.
         *
         * This is accomplished via the signaling mode below.
         */
        override fun visitRexOpErr(node: IRex.Op.Err, ctx: PType): Any {
            // Listener should have already received the error/warning. The listener should have already failed compilation.
            return operators.error(ctx)
        }

        override fun visitRelOpErr(node: org.partiql.planner.internal.ir.Rel.Op.Err, ctx: PType): Any {
            // Listener should have already received the error. This node is a dud. Registered error listeners should
            // have failed compilation already.
            return operators.scan(operators.error(ctx))
        }

        /**
         * Attach a PType to a rex.
         */
        private fun typed(type: PType, f: () -> Rex): Rex {
            val o = f()
            o.type = RexType(type)
            return o
        }

        // EXPRESSIONS

        override fun visitRex(node: IRex, ctx: PType): Rex = super.visitRexOp(node.op, node.type) as Rex

        override fun visitRexOp(node: IRex.Op, ctx: PType): Rex = super.visitRexOp(node, ctx) as Rex

        override fun visitRexOpTupleUnion(node: IRex.Op.TupleUnion, ctx: PType) = typed(ctx) {
            val args = node.args.map { visitRex(it, ctx) }
            operators.spread(args)
        }

        override fun visitRexOpSelect(node: IRex.Op.Select, ctx: PType) = typed(ctx) {
            val input = visitRel(node.rel, ctx)
            val constructor = visitRex(node.constructor, ctx)
            operators.select(input, constructor)
        }

        /**
         * TODO proper handling of subqueries in the planner.
         *
         * @param node
         * @param ctx
         * @return
         */
        override fun visitRexOpSubquery(node: IRex.Op.Subquery, ctx: PType) = typed(ctx) {
            val input = visitRel(node.rel, ctx)
            val constructor = visitRex(node.constructor, ctx)
            val isScalar = node.coercion == IRex.Op.Subquery.Coercion.SCALAR
            operators.subquery(input, constructor, isScalar)
        }

        override fun visitRexOpPivot(node: IRex.Op.Pivot, ctx: PType) = typed(ctx) {
            val input = visitRel(node.rel, ctx)
            val key = visitRex(node.key, ctx)
            val value = visitRex(node.value, ctx)
            operators.pivot(input, key, value)
        }

        override fun visitRexOpStruct(node: IRex.Op.Struct, ctx: PType) = typed(ctx) {
            val fields = node.fields.map { field(it) }
            operators.struct(fields)
        }

        override fun visitRexOpCollection(node: IRex.Op.Collection, ctx: PType) = typed(ctx) {
            val values = node.values.map { visitRex(it, ctx) }
            when (ctx.code()) {
                PType.ARRAY -> operators.array(values)
                PType.BAG -> operators.bag(values)
                else -> error("Expected bag or array, found ${ctx.name().lowercase()}")
            }
        }

        override fun visitRexOpCoalesce(node: IRex.Op.Coalesce, ctx: PType) = typed(ctx) {
            val args = node.args.map { visitRex(it, ctx) }
            operators.coalesce(args)
        }

        override fun visitRexOpNullif(node: IRex.Op.Nullif, ctx: PType) = typed(ctx) {
            val value = visitRex(node.value, ctx)
            val nullifier = visitRex(node.nullifier, ctx)
            operators.nullIf(value, nullifier)
        }

        override fun visitRexOpCase(node: IRex.Op.Case, ctx: PType) = typed(ctx) {
            val branches = node.branches.map { branch(it) }
            val default = visitRex(node.default, ctx)
            operators.caseWhen(null, branches, default)
        }

        override fun visitRexOpCallDynamic(node: IRex.Op.Call.Dynamic, ctx: PType) = typed(ctx) {
            // TODO assert on function name in plan typer .. here is not the place.
            val args = node.args.map { visitRex(it, ctx) }
            val fns = node.candidates.map { it.fn.signature }
            val name = node.candidates.first().fn.name.getName()
            operators.dispatch(name, fns, args)
        }

        override fun visitRexOpCallStatic(node: IRex.Op.Call.Static, ctx: PType) = typed(ctx) {
            val fn = node.fn
            val args = node.args.map { visitRex(it, ctx) }
            operators.call(fn, args)
        }

        override fun visitRexOpCallUnresolved(node: IRex.Op.Call.Unresolved, ctx: PType) = typed(ctx) {
            error("The Internal Node Rex.Op.Call.Unresolved should be converted to an Err Node during type resolution if resolution failed")
        }

        override fun visitRexOpCastUnresolved(node: IRex.Op.Cast.Unresolved, ctx: PType) = typed(ctx) {
            error("This should have been converted to an error node.")
        }

        override fun visitRexOpCastResolved(node: IRex.Op.Cast.Resolved, ctx: PType) = typed(ctx) {
            val operand = visitRex(node.arg, ctx)
            val target = node.cast.target
            operators.cast(operand, target)
        }

        override fun visitRexOpPathSymbol(node: IRex.Op.Path.Symbol, ctx: PType) = typed(ctx) {
            val operand = visitRex(node.root, ctx)
            val symbol = node.key
            operators.pathSymbol(operand, symbol)
        }

        override fun visitRexOpPathKey(node: IRex.Op.Path.Key, ctx: PType) = typed(ctx) {
            val operand = visitRex(node.root, ctx)
            val key = visitRex(node.key, ctx)
            operators.pathKey(operand, key)
        }

        override fun visitRexOpPathIndex(node: IRex.Op.Path.Index, ctx: PType) = typed(ctx) {
            val operand = visitRex(node.root, ctx)
            val index = visitRex(node.key, ctx)
            operators.pathIndex(operand, index)
        }

        override fun visitRexOpVarGlobal(node: IRex.Op.Var.Global, ctx: PType) = typed(ctx) {
            operators.table(node.ref.table)
        }

        override fun visitRexOpVarUnresolved(node: IRex.Op.Var.Unresolved, ctx: PType) = typed(ctx) {
            error("The Internal Plan Node Rex.Op.Var.Unresolved should be converted to an MISSING Node during type resolution if resolution failed")
        }

        override fun visitRexOpVarLocal(node: IRex.Op.Var.Local, ctx: PType) = typed(ctx) {
            val depth = node.depth
            val offset = node.ref
            operators.variable(depth, offset, ctx)
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpLit(node: IRex.Op.Lit, ctx: PType) = typed(ctx) {
            val value = node.value
            // TODO: PartiQLValue doesn't have a finite decimal type, so we need to specially handle this until we remove
            //  PartiQLValue.
            if (value is DecimalValue && ctx.code() == PType.DECIMAL) {
                when (val dec = value.value) {
                    null -> operators.lit(Datum.nullValue(ctx))
                    else -> operators.lit(Datum.decimal(dec, ctx.precision, ctx.scale))
                }
            } else {
                operators.lit(Datum.of(node.value))
            }
        }

        // RELATION OPERATORS

        override fun visitRel(node: IRel, ctx: PType): org.partiql.plan.rel.Rel =
            super.visitRelOp(node.op, ctx) as org.partiql.plan.rel.Rel

        override fun visitRelOp(node: IRel.Op, ctx: PType): org.partiql.plan.rel.Rel =
            super.visitRelOp(node, ctx) as org.partiql.plan.rel.Rel

        override fun visitRelOpAggregate(node: IRel.Op.Aggregate, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val calls = node.calls.map { visitRelOpAggregateCall(it, ctx) as RelAggregate.Measure }
            val groups = node.groups.map { visitRex(it, ctx) }
            return operators.aggregate(input, calls, groups)
        }

        override fun visitRelOpAggregateCallUnresolved(node: IRel.Op.Aggregate.Call.Unresolved, ctx: PType): Any {
            error("Unresolved aggregate call $node")
        }

        override fun visitRelOpAggregateCallResolved(node: IRel.Op.Aggregate.Call.Resolved, ctx: PType): Any {
            val agg = node.agg.signature
            val args = node.args.map { visitRex(it, ctx) }
            val isDistinct = node.setq == SetQuantifier.DISTINCT
            return RelAggregate.measure(agg, args, isDistinct)
        }

        override fun visitRelOpJoin(node: IRel.Op.Join, ctx: PType): Any {
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)
            val condition = visitRex(node.rex, ctx)

            val joinType = when (node.type) {
                IRel.Op.Join.Type.INNER -> JoinType.INNER()
                IRel.Op.Join.Type.LEFT -> JoinType.LEFT()
                IRel.Op.Join.Type.RIGHT -> JoinType.RIGHT()
                IRel.Op.Join.Type.FULL -> JoinType.FULL()
            }
            return operators.join(lhs, rhs, condition, joinType)
        }

        override fun visitRelOpExclude(node: IRel.Op.Exclude, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val paths = node.paths.mapNotNull { visitRelOpExcludePath(it, ctx) }
            return operators.exclude(input, paths)
        }

        override fun visitRelOpExcludePath(node: IRel.Op.Exclude.Path, ctx: PType): Exclusion? {
            val variable = visitRexOp(node.root, ctx) as? RexVar ?: return null
            val items = node.steps.map { visitRelOpExcludeStep(it, ctx) }
            return Exclusion(variable, items)
        }

        override fun visitRelOpExcludeStep(node: IRel.Op.Exclude.Step, ctx: PType): Exclusion.Item {
            val items = node.substeps.map { visitRelOpExcludeStep(it, ctx) }
            return when (node.type) {
                is IRel.Op.Exclude.Type.CollIndex -> Exclusion.collIndex(node.type.index, items)
                is IRel.Op.Exclude.Type.CollWildcard -> Exclusion.collWildcard(items)
                is IRel.Op.Exclude.Type.StructKey -> Exclusion.structKey(node.type.key, items)
                is IRel.Op.Exclude.Type.StructSymbol -> Exclusion.structSymbol(node.type.symbol, items)
                is IRel.Op.Exclude.Type.StructWildcard -> Exclusion.structWildCard(items)
            }
        }

        override fun visitRelOpProject(node: IRel.Op.Project, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val projections = node.projections.map { visitRex(it, ctx) }
            return operators.project(input, projections)
        }

        override fun visitRelOpOffset(node: IRel.Op.Offset, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val offset = visitRex(node.offset, ctx)
            return operators.offset(input, offset)
        }

        override fun visitRelOpLimit(node: IRel.Op.Limit, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val limit = visitRex(node.limit, ctx)
            return operators.limit(input, limit)
        }

        override fun visitRelOpIntersect(node: IRel.Op.Intersect, ctx: PType): Any {
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)
            val isAll = node.setq == SetQuantifier.ALL
            return operators.intersect(lhs, rhs, isAll)
        }

        override fun visitRelOpUnion(node: IRel.Op.Union, ctx: PType): Any {
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)
            val isAll = node.setq == SetQuantifier.ALL
            return operators.union(lhs, rhs, isAll)
        }

        override fun visitRelOpExcept(node: IRel.Op.Except, ctx: PType): Any {
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)
            val isAll = node.setq == SetQuantifier.ALL
            return operators.except(lhs, rhs, isAll)
        }

        override fun visitRelOpSort(node: IRel.Op.Sort, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val collations = node.specs.map { collation(it) }
            return operators.sort(input, collations)
        }

        override fun visitRelOpFilter(node: IRel.Op.Filter, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            val condition = visitRex(node.predicate, ctx)
            return operators.filter(input, condition)
        }

        override fun visitRelOpDistinct(node: IRel.Op.Distinct, ctx: PType): Any {
            val input = visitRel(node.input, ctx)
            return operators.distinct(input)
        }

        override fun visitRelOpUnpivot(node: IRel.Op.Unpivot, ctx: PType): Any {
            val input = visitRex(node.rex, ctx)
            return operators.unpivot(input)
        }

        override fun visitRelOpScanIndexed(node: IRel.Op.ScanIndexed, ctx: PType): Any {
            val input = visitRex(node.rex, ctx)
            return operators.iterate(input)
        }

        override fun visitRelOpScan(node: IRel.Op.Scan, ctx: PType): Any {
            val input = visitRex(node.rex, ctx)
            return operators.scan(input)
        }

        // HELPERS

        /**
         * TODO STANDARD COLLATION IMPLEMENTATION.
         */
        private fun collation(spec: IRel.Op.Sort.Spec): Collation {
            val rex = visitRex(spec.rex, spec.rex.type)
            val (order, nulls) = when (spec.order) {
                IRel.Op.Sort.Order.ASC_NULLS_LAST -> Collation.Order.ASC() to Collation.Nulls.LAST()
                IRel.Op.Sort.Order.ASC_NULLS_FIRST -> Collation.Order.ASC() to Collation.Nulls.FIRST()
                IRel.Op.Sort.Order.DESC_NULLS_LAST -> Collation.Order.DESC() to Collation.Nulls.LAST()
                IRel.Op.Sort.Order.DESC_NULLS_FIRST -> Collation.Order.DESC() to Collation.Nulls.FIRST()
            }
            return object : Collation {
                override fun getColumn(): Rex = rex
                override fun getOrder(): Collation.Order = order
                override fun getNulls(): Collation.Nulls = nulls
            }
        }

        private fun field(field: IRex.Op.Struct.Field): RexStruct.Field {
            val key = visitRex(field.k, field.k.type)
            val value = visitRex(field.v, field.v.type)
            return RexStruct.field(key, value)
        }

        private fun branch(branch: IRex.Op.Case.Branch): RexCase.Branch {
            val condition = visitRex(branch.condition, branch.condition.type)
            val result = visitRex(branch.rex, branch.rex.type)
            return RexCase.branch(condition, result)
        }
    }
}
