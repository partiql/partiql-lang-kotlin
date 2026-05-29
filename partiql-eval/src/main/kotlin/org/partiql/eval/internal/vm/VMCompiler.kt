package org.partiql.eval.internal.vm

import org.partiql.eval.Expr
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Mode
import org.partiql.eval.internal.helpers.checkInterrupted
import org.partiql.eval.internal.operator.Aggregate
import org.partiql.eval.internal.operator.rel.RelOpAggregate
import org.partiql.eval.internal.operator.rel.RelOpDistinct
import org.partiql.eval.internal.operator.rel.RelOpExceptAll
import org.partiql.eval.internal.operator.rel.RelOpExceptDistinct
import org.partiql.eval.internal.operator.rel.RelOpExclude
import org.partiql.eval.internal.operator.rel.RelOpFilter
import org.partiql.eval.internal.operator.rel.RelOpIntersectAll
import org.partiql.eval.internal.operator.rel.RelOpIntersectDistinct
import org.partiql.eval.internal.operator.rel.RelOpIterate
import org.partiql.eval.internal.operator.rel.RelOpIteratePermissive
import org.partiql.eval.internal.operator.rel.RelOpJoinInner
import org.partiql.eval.internal.operator.rel.RelOpJoinOuterFull
import org.partiql.eval.internal.operator.rel.RelOpJoinOuterLeft
import org.partiql.eval.internal.operator.rel.RelOpJoinOuterRight
import org.partiql.eval.internal.operator.rel.RelOpLimit
import org.partiql.eval.internal.operator.rel.RelOpOffset
import org.partiql.eval.internal.operator.rel.RelOpProject
import org.partiql.eval.internal.operator.rel.RelOpScan
import org.partiql.eval.internal.operator.rel.RelOpScanPermissive
import org.partiql.eval.internal.operator.rel.RelOpSort
import org.partiql.eval.internal.operator.rel.RelOpUnionAll
import org.partiql.eval.internal.operator.rel.RelOpUnionDistinct
import org.partiql.eval.internal.operator.rel.RelOpUnpivot
import org.partiql.eval.internal.operator.rel.RelOpWindow
import org.partiql.eval.internal.operator.rex.ExprArray
import org.partiql.eval.internal.operator.rex.ExprBag
import org.partiql.eval.internal.operator.rex.ExprCall
import org.partiql.eval.internal.operator.rex.ExprCallDynamic
import org.partiql.eval.internal.operator.rex.ExprCaseBranch
import org.partiql.eval.internal.operator.rex.ExprCaseSearched
import org.partiql.eval.internal.operator.rex.ExprCast
import org.partiql.eval.internal.operator.rex.ExprCoalesce
import org.partiql.eval.internal.operator.rex.ExprError
import org.partiql.eval.internal.operator.rex.ExprLit
import org.partiql.eval.internal.operator.rex.ExprMissing
import org.partiql.eval.internal.operator.rex.ExprNullIf
import org.partiql.eval.internal.operator.rex.ExprPathIndex
import org.partiql.eval.internal.operator.rex.ExprPathKey
import org.partiql.eval.internal.operator.rex.ExprPathSymbol
import org.partiql.eval.internal.operator.rex.ExprPermissive
import org.partiql.eval.internal.operator.rex.ExprPivot
import org.partiql.eval.internal.operator.rex.ExprPivotPermissive
import org.partiql.eval.internal.operator.rex.ExprSelect
import org.partiql.eval.internal.operator.rex.ExprSpread
import org.partiql.eval.internal.operator.rex.ExprStructField
import org.partiql.eval.internal.operator.rex.ExprStructPermissive
import org.partiql.eval.internal.operator.rex.ExprStructStrict
import org.partiql.eval.internal.operator.rex.ExprSubquery
import org.partiql.eval.internal.operator.rex.ExprSubqueryRow
import org.partiql.eval.internal.operator.rex.ExprTable
import org.partiql.eval.internal.operator.rex.ExprVar
import org.partiql.eval.internal.window.WindowBuiltIns
import org.partiql.plan.Collation
import org.partiql.plan.JoinType
import org.partiql.plan.Operator
import org.partiql.plan.OperatorVisitor
import org.partiql.plan.rel.Rel
import org.partiql.plan.rel.RelAggregate
import org.partiql.plan.rel.RelDistinct
import org.partiql.plan.rel.RelExcept
import org.partiql.plan.rel.RelExclude
import org.partiql.plan.rel.RelFilter
import org.partiql.plan.rel.RelIntersect
import org.partiql.plan.rel.RelIterate
import org.partiql.plan.rel.RelJoin
import org.partiql.plan.rel.RelLimit
import org.partiql.plan.rel.RelOffset
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rel.RelScan
import org.partiql.plan.rel.RelSort
import org.partiql.plan.rel.RelUnion
import org.partiql.plan.rel.RelUnpivot
import org.partiql.plan.rel.RelWindow
import org.partiql.plan.rel.RelWith
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexArray
import org.partiql.plan.rex.RexBag
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexCallRef
import org.partiql.plan.rex.RexCase
import org.partiql.plan.rex.RexCast
import org.partiql.plan.rex.RexCoalesce
import org.partiql.plan.rex.RexDispatch
import org.partiql.plan.rex.RexDispatchRef
import org.partiql.plan.rex.RexError
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexNullIf
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.plan.rex.RexPivot
import org.partiql.plan.rex.RexSelect
import org.partiql.plan.rex.RexSpread
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexSubquery
import org.partiql.plan.rex.RexTable
import org.partiql.plan.rex.RexTableRef
import org.partiql.plan.rex.RexVar
import org.partiql.spi.catalog.ExecutionCatalog
import org.partiql.spi.types.PType

/**
 * Compiles a plan tree into a fresh physical operator tree, resolving references via [ExecutionCatalog].
 *
 * Builds a new operator tree per invocation — no shared mutable state.
 */
internal class VMCompiler(
    private val catalogs: Array<ExecutionCatalog>,
    private val mode: Mode,
) : OperatorVisitor<Expr, Unit> {

    private val MODE = mode.code()

    fun compile(rex: Rex): ExprValue {
        checkInterrupted()
        return rex.accept(this, Unit) as ExprValue
    }

    private fun compileRel(rel: Rel): ExprRelation {
        checkInterrupted()
        return rel.accept(this, Unit) as ExprRelation
    }

    override fun defaultReturn(operator: Operator, ctx: Unit): Expr {
        error("No VM compiler strategy for: ${operator::class.java.simpleName}")
    }

    // --- REX (Ref-based nodes) ---

    override fun visitTableRef(rex: RexTableRef, ctx: Unit): ExprValue {
        val table = catalogs[rex.catalogId].getTable(rex.tableId)
        return ExprTable(table)
    }

    override fun visitCallRef(rex: RexCallRef, ctx: Unit): ExprValue {
        val fn = catalogs[rex.catalogId].getFn(rex.fnId)
        val args = Array(rex.args.size) { i -> compile(rex.args[i]).catch() }
        return ExprCall(fn, args)
    }

    override fun visitDispatchRef(rex: RexDispatchRef, ctx: Unit): ExprValue {
        val catalog = catalogs[rex.catalogId]
        val candidates = Array(rex.fnIds.size) { i ->
            catalog.getFnOverload(rex.fnIds[i])
        }
        val args = rex.args.map { compile(it).catch() }.toTypedArray()
        return ExprCallDynamic(rex.name, candidates, args)
    }

    // --- REX (existing nodes that may still appear) ---

    override fun visitTable(rex: RexTable, ctx: Unit): ExprValue {
        return ExprTable(rex.table)
    }

    override fun visitCall(rex: RexCall, ctx: Unit): ExprValue {
        val fn = rex.function
        val args = rex.args
        val catch = fn.signature.parameters.any { it.type.code() == PType.DYNAMIC }
        return when (catch) {
            true -> ExprCall(fn, Array(args.size) { i -> compile(args[i]).catch() })
            else -> ExprCall(fn, Array(args.size) { i -> compile(args[i]) })
        }
    }

    override fun visitDispatch(rex: RexDispatch, ctx: Unit): ExprValue {
        val fns = rex.functions
        val candidates = Array(fns.size) { fns[it] }
        val args = rex.args.map { compile(it).catch() }.toTypedArray()
        return ExprCallDynamic(rex.name, candidates, args)
    }

    override fun visitVar(rex: RexVar, ctx: Unit): ExprValue {
        return ExprVar(rex.scope, rex.offset)
    }

    override fun visitLit(rex: RexLit, ctx: Unit): ExprValue {
        return ExprLit(rex.datum)
    }

    override fun visitError(rex: RexError, ctx: Unit): ExprValue {
        return when (MODE) {
            Mode.PERMISSIVE -> ExprMissing(PType.unknown())
            Mode.STRICT -> ExprError()
            else -> throw IllegalStateException("Unsupported execution mode: $MODE")
        }
    }

    override fun visitArray(rex: RexArray, ctx: Unit): ExprValue {
        val values = rex.values.map { compile(it).catch() }
        return ExprArray(values)
    }

    override fun visitBag(rex: RexBag, ctx: Unit): ExprValue {
        val values = rex.values.map { compile(it).catch() }
        return ExprBag(values)
    }

    override fun visitCase(rex: RexCase, ctx: Unit): ExprValue {
        val branches = rex.branches.map {
            val value = compile(it.condition).catch()
            val result = compile(it.result)
            ExprCaseBranch(value, result)
        }
        val default = rex.default?.let { compile(it) }
        return ExprCaseSearched(branches, default)
    }

    override fun visitCast(rex: RexCast, ctx: Unit): ExprValue {
        val operand = compile(rex.operand)
        return ExprCast(operand, rex.target)
    }

    override fun visitCoalesce(rex: RexCoalesce, ctx: Unit): ExprValue {
        val args = rex.args.map { compile(it).catch() }.toTypedArray()
        return ExprCoalesce(args)
    }

    override fun visitNullIf(rex: RexNullIf, ctx: Unit): ExprValue {
        val value = compile(rex.v1).catch()
        val nullifier = compile(rex.v2).catch()
        return ExprNullIf(value, nullifier)
    }

    override fun visitPathIndex(rex: RexPathIndex, ctx: Unit): ExprValue {
        val operand = compile(rex.operand)
        val index = compile(rex.index)
        return ExprPathIndex(operand, index)
    }

    override fun visitPathKey(rex: RexPathKey, ctx: Unit): ExprValue {
        val operand = compile(rex.operand)
        val key = compile(rex.key)
        return ExprPathKey(operand, key).catch()
    }

    override fun visitPathSymbol(rex: RexPathSymbol, ctx: Unit): ExprValue {
        val operand = compile(rex.operand)
        val symbol = rex.symbol
        return ExprPathSymbol(operand, symbol).catch()
    }

    override fun visitPivot(rex: RexPivot, ctx: Unit): ExprValue {
        val input = compileRel(rex.input)
        val key = compile(rex.key)
        val value = compile(rex.value)
        return when (MODE) {
            Mode.PERMISSIVE -> ExprPivotPermissive(input, key, value)
            Mode.STRICT -> ExprPivot(input, key, value)
            else -> throw IllegalStateException("Unsupported execution mode: $MODE")
        }
    }

    override fun visitSelect(rex: RexSelect, ctx: Unit): ExprValue {
        val input = compileRel(rex.input)
        val constructor = compile(rex.constructor).catch()
        val ordered = rex.input.type.isOrdered
        return ExprSelect(input, constructor, ordered)
    }

    override fun visitStruct(rex: RexStruct, ctx: Unit): ExprValue {
        val fields = rex.fields.map {
            val k = compile(it.key)
            val v = compile(it.value).catch()
            ExprStructField(k, v)
        }
        return when (MODE) {
            Mode.PERMISSIVE -> ExprStructPermissive(fields)
            Mode.STRICT -> ExprStructStrict(fields)
            else -> throw IllegalStateException("Unsupported execution mode: $MODE")
        }
    }

    override fun visitSubquery(rex: RexSubquery, ctx: Unit): ExprValue {
        val rel = compileRel(rex.input)
        val constructor = compile(rex.constructor)
        return when (rex.isScalar) {
            true -> ExprSubquery(rel, constructor)
            else -> ExprSubqueryRow(rel, constructor)
        }
    }

    override fun visitSpread(rex: RexSpread, ctx: Unit): ExprValue {
        val args = rex.args.map { compile(it) }.toTypedArray()
        return ExprSpread(args)
    }

    // --- REL ---

    override fun visitScan(rel: RelScan, ctx: Unit): ExprRelation {
        val input = compile(rel.rex)
        return when (MODE) {
            Mode.PERMISSIVE -> RelOpScanPermissive(input.catch())
            Mode.STRICT -> RelOpScan(input)
            else -> throw IllegalStateException("Unsupported execution mode: $MODE")
        }
    }

    override fun visitIterate(rel: RelIterate, ctx: Unit): ExprRelation {
        val input = compile(rel.rex)
        return when (MODE) {
            Mode.PERMISSIVE -> RelOpIteratePermissive(input.catch())
            Mode.STRICT -> RelOpIterate(input)
            else -> throw IllegalStateException("Unsupported execution mode: $MODE")
        }
    }

    override fun visitUnpivot(rel: RelUnpivot, ctx: Unit): ExprRelation {
        val input = compile(rel.rex)
        return when (MODE) {
            Mode.PERMISSIVE -> RelOpUnpivot.Permissive(input)
            Mode.STRICT -> RelOpUnpivot.Strict(input)
            else -> throw IllegalStateException("Unsupported execution mode: $MODE")
        }
    }

    override fun visitFilter(rel: RelFilter, ctx: Unit): ExprRelation {
        val input = compileRel(rel.input)
        val predicate = compile(rel.predicate).catch()
        return RelOpFilter(input, predicate)
    }

    override fun visitProject(rel: RelProject, ctx: Unit): ExprRelation {
        val input = compileRel(rel.input)
        val projections = rel.projections.map { compile(it).catch() }
        return RelOpProject(input, projections)
    }

    override fun visitJoin(rel: RelJoin, ctx: Unit): ExprRelation {
        val lhs = compileRel(rel.left)
        val rhs = compileRel(rel.right)
        val condition = compile(rel.condition)
        val lhsType = rel.left.type
        val rhsType = rel.right.type
        return when (rel.joinType.code()) {
            JoinType.INNER -> RelOpJoinInner(lhs, rhs, condition)
            JoinType.LEFT -> RelOpJoinOuterLeft(lhs, rhs, condition, rhsType)
            JoinType.RIGHT -> RelOpJoinOuterRight(lhs, rhs, condition, lhsType)
            JoinType.FULL -> RelOpJoinOuterFull(lhs, rhs, condition, lhsType, rhsType)
            else -> error("Unsupported join type: ${rel.joinType}")
        }
    }

    override fun visitSort(rel: RelSort, ctx: Unit): ExprRelation {
        val input = compileRel(rel.input)
        val collations = rel.collations.map { collation(it) }
        return RelOpSort(input, collations)
    }

    override fun visitDistinct(rel: RelDistinct, ctx: Unit): ExprRelation {
        val input = compileRel(rel.input)
        return RelOpDistinct(input)
    }

    override fun visitLimit(rel: RelLimit, ctx: Unit): ExprRelation {
        val input = compileRel(rel.input)
        val limit = compile(rel.limit)
        return RelOpLimit(input, limit)
    }

    override fun visitOffset(rel: RelOffset, ctx: Unit): ExprRelation {
        val input = compileRel(rel.input)
        val offset = compile(rel.offset)
        return RelOpOffset(input, offset)
    }

    override fun visitAggregate(rel: RelAggregate, ctx: Unit): ExprRelation {
        val input = compileRel(rel.input)
        val groups = rel.groups.map { compile(it).catch() }
        val measureRefs = rel.measureRefs
        if (measureRefs.isNotEmpty()) {
            val aggs = measureRefs.map { ref ->
                val agg = catalogs[ref.catalogId].getAgg(ref.aggId)
                val args = ref.args.map { compile(it).catch() }
                Aggregate(agg, args, ref.isDistinct)
            }
            return RelOpAggregate(input, aggs, groups)
        }
        val aggs = rel.measures.map { measure ->
            val args = measure.args.map { compile(it).catch() }
            Aggregate(measure.agg, args, measure.isDistinct)
        }
        return RelOpAggregate(input, aggs, groups)
    }

    override fun visitUnion(rel: RelUnion, ctx: Unit): ExprRelation {
        val lhs = compileRel(rel.left)
        val rhs = compileRel(rel.right)
        return when (rel.isAll) {
            true -> RelOpUnionAll(lhs, rhs)
            else -> RelOpUnionDistinct(lhs, rhs)
        }
    }

    override fun visitIntersect(rel: RelIntersect, ctx: Unit): ExprRelation {
        val lhs = compileRel(rel.left)
        val rhs = compileRel(rel.right)
        return when (rel.isAll) {
            true -> RelOpIntersectAll(lhs, rhs)
            else -> RelOpIntersectDistinct(lhs, rhs)
        }
    }

    override fun visitExcept(rel: RelExcept, ctx: Unit): ExprRelation {
        val lhs = compileRel(rel.left)
        val rhs = compileRel(rel.right)
        return when (rel.isAll) {
            true -> RelOpExceptAll(lhs, rhs)
            else -> RelOpExceptDistinct(lhs, rhs)
        }
    }

    override fun visitExclude(rel: RelExclude, ctx: Unit): ExprRelation {
        val input = compileRel(rel.input)
        return RelOpExclude(input, rel.exclusions)
    }

    @Suppress("DEPRECATION")
    override fun visitWindow(rel: RelWindow, ctx: Unit): ExprRelation {
        val input = compileRel(rel.input)
        val functions = rel.windowFunctions.map {
            val args = it.arguments.map { arg -> compile(arg).catch() }
            WindowBuiltIns.get(it.signature, args)
        }
        val partitionBy = rel.partitions.map { compile(it) }
        val sortBy = rel.collations.map { collation(it) }
        val realSortBy = partitionBy.map {
            org.partiql.eval.internal.operator.rel.Collation(it, false, false)
        } + sortBy
        val sorted = RelOpSort(input, realSortBy)
        return RelOpWindow(sorted, functions, partitionBy, sortBy)
    }

    override fun visitWith(rel: RelWith, ctx: Unit): ExprRelation {
        return compileRel(rel.input)
    }

    // --- Helpers ---

    private fun collation(it: Collation): org.partiql.eval.internal.operator.rel.Collation {
        val expr = compile(it.column)
        val desc = it.order.code() == Collation.Order.DESC
        val last = it.nulls.code() == Collation.Nulls.LAST
        return org.partiql.eval.internal.operator.rel.Collation(expr, desc, last)
    }

    private fun ExprValue.catch(): ExprValue = when (MODE) {
        Mode.PERMISSIVE -> ExprPermissive(this)
        Mode.STRICT -> this
        else -> throw IllegalStateException("Unsupported execution mode: $MODE")
    }
}
