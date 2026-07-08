package org.partiql.eval.internal.compiler

import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Mode
import org.partiql.eval.internal.helpers.checkInterrupted
import org.partiql.eval.internal.operator.Aggregate
import org.partiql.eval.internal.operator.rel.Collation
import org.partiql.eval.internal.operator.rel.RelOpAggregate
import org.partiql.eval.internal.operator.rel.RelOpCorrelateInner
import org.partiql.eval.internal.operator.rel.RelOpCorrelateLeft
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
import org.partiql.eval.internal.operator.rex.ExprMapConstruct
import org.partiql.eval.internal.operator.rex.ExprMapConstructDynamic
import org.partiql.eval.internal.operator.rex.ExprMapConstructDynamicStrict
import org.partiql.eval.internal.operator.rex.ExprMapConstructStrict
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
import org.partiql.eval.internal.plan.ExecutionPlanImpl
import org.partiql.eval.internal.plan.PCollation
import org.partiql.eval.internal.plan.PExpr
import org.partiql.eval.internal.plan.PJoinType
import org.partiql.eval.internal.plan.PRel
import org.partiql.eval.internal.window.WindowBuiltIns
import org.partiql.spi.catalog.ExecutionCatalog
import org.partiql.spi.types.PType

/**
 * Compiles an [ExecutionPlanImpl] into a fresh physical operator tree.
 * Each invocation produces new operator instances — no shared mutable state.
 */
internal class OperatorCompiler(
    private val catalogs: Array<ExecutionCatalog>,
    private val mode: Mode,
) {
    private val MODE = mode.code()

    fun compile(plan: ExecutionPlanImpl): ExprValue = compile(plan.root).catch()

    fun compile(expr: PExpr): ExprValue {
        checkInterrupted()
        return when (expr) {
            is PExpr.Lit -> ExprLit(expr.value)
            is PExpr.Var -> ExprVar(expr.depth, expr.offset)
            is PExpr.TableRef -> {
                val table = catalogs[expr.catalogId].getTable(expr.tableId)
                ExprTable(table)
            }
            is PExpr.TableDirect -> ExprTable(expr.table)
            is PExpr.Call -> {
                val args = kotlin.Array(expr.args.size) { i -> compile(expr.args[i]).catch() }
                ExprCall(expr.fn, args)
            }
            is PExpr.DynamicCall -> {
                val candidates = kotlin.Array(expr.overloads.size) { expr.overloads[it] }
                val args = expr.args.map { compile(it).catch() }.toTypedArray()
                ExprCallDynamic(expr.name, candidates, args)
            }
            is PExpr.Cast -> ExprCast(compile(expr.operand), expr.target)
            is PExpr.Case -> {
                val branches = expr.branches.map { ExprCaseBranch(compile(it.condition).catch(), compile(it.result)) }
                val default = expr.default?.let { compile(it) }
                ExprCaseSearched(branches, default)
            }
            is PExpr.NullIf -> ExprNullIf(compile(expr.v1).catch(), compile(expr.v2).catch())
            is PExpr.Coalesce -> ExprCoalesce(expr.args.map { compile(it).catch() }.toTypedArray())
            is PExpr.Array -> ExprArray(expr.values.map { compile(it).catch() })
            is PExpr.Bag -> ExprBag(expr.values.map { compile(it).catch() })
            is PExpr.Struct -> {
                val fields = expr.fields.map { ExprStructField(compile(it.key), compile(it.value).catch()) }
                when (MODE) {
                    Mode.PERMISSIVE -> ExprStructPermissive(fields)
                    Mode.STRICT -> ExprStructStrict(fields)
                    else -> error("Unsupported mode: $MODE")
                }
            }
            is PExpr.Map -> {
                val entries = expr.entries.map { ExprStructField(compile(it.key), compile(it.value).catch()) }
                when (MODE) {
                    Mode.PERMISSIVE -> ExprMapConstruct(expr.keyType, expr.valueType, entries)
                    Mode.STRICT -> ExprMapConstructStrict(expr.keyType, expr.valueType, entries)
                    else -> error("Unsupported mode: $MODE")
                }
            }
            is PExpr.MapDynamic -> {
                val entries = expr.entries.map { ExprStructField(compile(it.key), compile(it.value).catch()) }
                when (MODE) {
                    Mode.PERMISSIVE -> ExprMapConstructDynamic(entries)
                    Mode.STRICT -> ExprMapConstructDynamicStrict(entries)
                    else -> error("Unsupported mode: $MODE")
                }
            }
            is PExpr.Spread -> ExprSpread(expr.args.map { compile(it) }.toTypedArray())
            is PExpr.Select -> {
                val input = compileRel(expr.input)
                val constructor = compile(expr.constructor).catch()
                ExprSelect(input, constructor, expr.ordered)
            }
            is PExpr.Pivot -> {
                val input = compileRel(expr.input)
                val key = compile(expr.key)
                val value = compile(expr.value)
                when (MODE) {
                    Mode.PERMISSIVE -> ExprPivotPermissive(input, key, value)
                    Mode.STRICT -> ExprPivot(input, key, value)
                    else -> error("Unsupported mode: $MODE")
                }
            }
            is PExpr.Subquery -> {
                val rel = compileRel(expr.input)
                val constructor = compile(expr.constructor)
                when (expr.scalar) {
                    true -> ExprSubquery(rel, constructor)
                    else -> ExprSubqueryRow(rel, constructor)
                }
            }
            is PExpr.PathKey -> ExprPathKey(compile(expr.root), compile(expr.key)).catch()
            is PExpr.PathIndex -> ExprPathIndex(compile(expr.root), compile(expr.index))
            is PExpr.PathSymbol -> ExprPathSymbol(compile(expr.root), expr.symbol).catch()
            is PExpr.Error -> when (MODE) {
                Mode.PERMISSIVE -> ExprMissing(PType.unknown())
                Mode.STRICT -> ExprError()
                else -> error("Unsupported mode: $MODE")
            }
            is PExpr.Custom -> expr.factory()
        }
    }

    fun compileRel(rel: PRel): ExprRelation {
        checkInterrupted()
        return when (rel) {
            is PRel.Scan -> {
                val input = compile(rel.expr)
                when (MODE) {
                    Mode.PERMISSIVE -> RelOpScanPermissive(input.catch())
                    Mode.STRICT -> RelOpScan(input)
                    else -> error("Unsupported mode: $MODE")
                }
            }
            is PRel.Iterate -> {
                val input = compile(rel.expr)
                when (MODE) {
                    Mode.PERMISSIVE -> RelOpIteratePermissive(input.catch())
                    Mode.STRICT -> RelOpIterate(input)
                    else -> error("Unsupported mode: $MODE")
                }
            }
            is PRel.Unpivot -> {
                val input = compile(rel.expr)
                when (MODE) {
                    Mode.PERMISSIVE -> RelOpUnpivot.Permissive(input)
                    Mode.STRICT -> RelOpUnpivot.Strict(input)
                    else -> error("Unsupported mode: $MODE")
                }
            }
            is PRel.Filter -> RelOpFilter(compileRel(rel.input), compile(rel.predicate).catch())
            is PRel.Project -> RelOpProject(compileRel(rel.input), rel.projections.map { compile(it).catch() })
            is PRel.Join -> {
                val lhs = compileRel(rel.lhs)
                val rhs = compileRel(rel.rhs)
                val condition = compile(rel.condition)
                val lhsType = rel.lhs.type!!
                val rhsType = rel.rhs.type!!
                when (rel.joinType) {
                    PJoinType.INNER -> RelOpJoinInner(lhs, rhs, condition)
                    PJoinType.LEFT -> RelOpJoinOuterLeft(lhs, rhs, condition, rhsType)
                    PJoinType.RIGHT -> RelOpJoinOuterRight(lhs, rhs, condition, lhsType)
                    PJoinType.FULL -> RelOpJoinOuterFull(lhs, rhs, condition, lhsType, rhsType)
                }
            }
            is PRel.Correlate -> {
                val lhs = compileRel(rel.lhs)
                val rhs = compileRel(rel.rhs)
                val rhsType = rel.rhs.type!!
                when (rel.joinType) {
                    PJoinType.INNER -> RelOpCorrelateInner(lhs, rhs)
                    PJoinType.LEFT -> RelOpCorrelateLeft(lhs, rhs, rhsType)
                    else -> error("Unsupported correlate join type: ${rel.joinType}")
                }
            }
            is PRel.Sort -> RelOpSort(compileRel(rel.input), rel.collations.map { toCollation(it) })
            is PRel.Distinct -> RelOpDistinct(compileRel(rel.input))
            is PRel.Limit -> RelOpLimit(compileRel(rel.input), compile(rel.limit))
            is PRel.Offset -> RelOpOffset(compileRel(rel.input), compile(rel.offset))
            is PRel.Aggregate -> {
                val input = compileRel(rel.input)
                val groups = rel.groups.map { compile(it).catch() }
                val aggs = rel.measures.map { measure ->
                    val args = measure.args.map { compile(it).catch() }
                    Aggregate(measure.agg, args, measure.distinct)
                }
                RelOpAggregate(input, aggs, groups)
            }
            is PRel.Union -> {
                val lhs = compileRel(rel.lhs)
                val rhs = compileRel(rel.rhs)
                if (rel.all) RelOpUnionAll(lhs, rhs) else RelOpUnionDistinct(lhs, rhs)
            }
            is PRel.Intersect -> {
                val lhs = compileRel(rel.lhs)
                val rhs = compileRel(rel.rhs)
                if (rel.all) RelOpIntersectAll(lhs, rhs) else RelOpIntersectDistinct(lhs, rhs)
            }
            is PRel.Except -> {
                val lhs = compileRel(rel.lhs)
                val rhs = compileRel(rel.rhs)
                if (rel.all) RelOpExceptAll(lhs, rhs) else RelOpExceptDistinct(lhs, rhs)
            }
            is PRel.Exclude -> RelOpExclude(compileRel(rel.input), rel.exclusions)
            is PRel.Custom -> rel.factory()
            is PRel.Window -> {
                val input = compileRel(rel.input)
                val functions = rel.functions.map { wf ->
                    val args = wf.args.map { compile(it).catch() }
                    WindowBuiltIns.get(wf.signature, args)
                }
                val partitionBy = rel.partitions.map { compile(it) }
                val sortBy = rel.sorts.map { toCollation(it) }
                val realSortBy = partitionBy.map { Collation(it, false, false) } + sortBy
                val sorted = RelOpSort(input, realSortBy)
                RelOpWindow(sorted, functions, partitionBy, sortBy)
            }
        }
    }

    private fun toCollation(c: PCollation): Collation =
        Collation(compile(c.expr), c.desc, c.nullsLast)

    private fun ExprValue.catch(): ExprValue = when (MODE) {
        Mode.PERMISSIVE -> ExprPermissive(this)
        Mode.STRICT -> this
        else -> error("Unsupported mode: $MODE")
    }
}
