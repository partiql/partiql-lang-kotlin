package org.partiql.eval.internal.compiler

import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Mode
import org.partiql.eval.compiler.Match
import org.partiql.eval.compiler.Strategy
import org.partiql.eval.internal.plan.ExecutionPlanImpl
import org.partiql.eval.internal.plan.PCollation
import org.partiql.eval.internal.plan.PExpr
import org.partiql.eval.internal.plan.PJoinType
import org.partiql.eval.internal.plan.PMeasure
import org.partiql.eval.internal.plan.PRel
import org.partiql.eval.internal.plan.PWindowFn
import org.partiql.plan.Action
import org.partiql.plan.Collation
import org.partiql.plan.JoinType
import org.partiql.plan.Operand
import org.partiql.plan.Operator
import org.partiql.plan.OperatorVisitor
import org.partiql.plan.Plan
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
import org.partiql.plan.rex.RexCase
import org.partiql.plan.rex.RexCast
import org.partiql.plan.rex.RexCoalesce
import org.partiql.plan.rex.RexDispatch
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

/**
 * Transforms a public [Plan] into an internal [ExecutionPlanImpl].
 */
internal class PlanToExecTransform(
    private val strategies: List<Strategy> = emptyList(),
    private val mode: Mode = Mode.PERMISSIVE(),
) : OperatorVisitor<Any, Unit> {

    fun transform(plan: Plan): ExecutionPlanImpl {
        val action = plan.action
        if (action !is Action.Query) {
            error("Only query statements are supported")
        }
        val root = visitRex(action.rex)
        return ExecutionPlanImpl(root, mode)
    }

    private fun visitRex(rex: Rex): PExpr {
        if (strategies.isNotEmpty()) {
            val custom = tryStrategies(rex)
            if (custom != null) return custom as PExpr
        }
        return rex.accept(this, Unit) as PExpr
    }

    private fun visitRel(rel: Rel): PRel {
        if (strategies.isNotEmpty()) {
            val custom = tryStrategies(rel)
            if (custom != null) return custom as PRel
        }
        return rel.accept(this, Unit) as PRel
    }

    private fun tryStrategies(operator: Operator): Any? {
        for (strategy in strategies) {
            if (strategy.pattern.matches(operator)) {
                val operand = Operand.single(operator)
                val match = Match(operand)
                // The callback compiles a child operator to a physical Expr.
                // It transforms to internal IR then compiles with an OperatorCompiler.
                val callback = Strategy.Callback { op ->
                    val node = op.accept(this, Unit)
                    val compiler = OperatorCompiler(emptyArray(), mode)
                    when (node) {
                        is PExpr -> compiler.compile(node)
                        is PRel -> compiler.compileRel(node)
                        else -> error("Unexpected node type from strategy callback")
                    }
                }
                val supplier = strategy.applyFactory(match, mode, callback)
                return if (operator is Rel) {
                    PRel.Custom(factory = { supplier.get() as ExprRelation })
                } else {
                    PExpr.Custom(factory = { supplier.get() as ExprValue })
                }
            }
        }
        return null
    }

    override fun defaultReturn(operator: Operator, ctx: Unit): Any {
        error("Unsupported plan operator: ${operator::class.java.simpleName}")
    }

    override fun defaultVisit(operator: Operator, ctx: Unit): Any {
        return defaultReturn(operator, ctx)
    }

    // --- Rex ---

    override fun visitTableRef(rex: RexTableRef, ctx: Unit): Any =
        PExpr.TableRef(rex.catalogId, rex.tableId)

    @Suppress("DEPRECATION")
    override fun visitTable(rex: RexTable, ctx: Unit): Any {
        return PExpr.TableDirect(rex.getTable())
    }

    override fun visitCall(rex: RexCall, ctx: Unit): Any {
        val fn = rex.function
        val args = rex.args.map { visitRex(it) }
        return PExpr.Call(fn, args)
    }

    override fun visitDispatch(rex: RexDispatch, ctx: Unit): Any {
        return PExpr.DynamicCall(rex.name, rex.functions, rex.args.map { visitRex(it) })
    }

    override fun visitLit(rex: RexLit, ctx: Unit): Any = PExpr.Lit(rex.datum)

    override fun visitVar(rex: RexVar, ctx: Unit): Any = PExpr.Var(rex.scope, rex.offset)

    override fun visitError(rex: RexError, ctx: Unit): Any = PExpr.Error(rex.type.pType)

    override fun visitArray(rex: RexArray, ctx: Unit): Any =
        PExpr.Array(rex.values.map { visitRex(it) })

    override fun visitBag(rex: RexBag, ctx: Unit): Any =
        PExpr.Bag(rex.values.map { visitRex(it) })

    override fun visitCase(rex: RexCase, ctx: Unit): Any {
        val branches = rex.branches.map { PExpr.Branch(visitRex(it.condition), visitRex(it.result)) }
        val default = rex.default?.let { visitRex(it) }
        return PExpr.Case(branches, default)
    }

    override fun visitCast(rex: RexCast, ctx: Unit): Any =
        PExpr.Cast(visitRex(rex.operand), rex.target)

    override fun visitCoalesce(rex: RexCoalesce, ctx: Unit): Any =
        PExpr.Coalesce(rex.args.map { visitRex(it) })

    override fun visitNullIf(rex: RexNullIf, ctx: Unit): Any =
        PExpr.NullIf(visitRex(rex.v1), visitRex(rex.v2))

    override fun visitPathIndex(rex: RexPathIndex, ctx: Unit): Any =
        PExpr.PathIndex(visitRex(rex.operand), visitRex(rex.index))

    override fun visitPathKey(rex: RexPathKey, ctx: Unit): Any =
        PExpr.PathKey(visitRex(rex.operand), visitRex(rex.key))

    override fun visitPathSymbol(rex: RexPathSymbol, ctx: Unit): Any =
        PExpr.PathSymbol(visitRex(rex.operand), rex.symbol)

    override fun visitPivot(rex: RexPivot, ctx: Unit): Any =
        PExpr.Pivot(visitRel(rex.input), visitRex(rex.key), visitRex(rex.value))

    override fun visitSelect(rex: RexSelect, ctx: Unit): Any =
        PExpr.Select(visitRel(rex.input), visitRex(rex.constructor), rex.input.type.isOrdered)

    override fun visitStruct(rex: RexStruct, ctx: Unit): Any =
        PExpr.Struct(rex.fields.map { PExpr.Field(visitRex(it.key), visitRex(it.value)) })

    override fun visitSubquery(rex: RexSubquery, ctx: Unit): Any =
        PExpr.Subquery(visitRel(rex.input), visitRex(rex.constructor), rex.isScalar)

    override fun visitSpread(rex: RexSpread, ctx: Unit): Any =
        PExpr.Spread(rex.args.map { visitRex(it) })

    // --- Rel ---

    override fun visitScan(rel: RelScan, ctx: Unit): Any =
        PRel.Scan(visitRex(rel.rex), rel.type)

    override fun visitIterate(rel: RelIterate, ctx: Unit): Any =
        PRel.Iterate(visitRex(rel.rex), rel.type)

    override fun visitUnpivot(rel: RelUnpivot, ctx: Unit): Any =
        PRel.Unpivot(visitRex(rel.rex), rel.type)

    override fun visitFilter(rel: RelFilter, ctx: Unit): Any =
        PRel.Filter(visitRel(rel.input), visitRex(rel.predicate), rel.type)

    override fun visitProject(rel: RelProject, ctx: Unit): Any =
        PRel.Project(visitRel(rel.input), rel.projections.map { visitRex(it) }, rel.type)

    override fun visitJoin(rel: RelJoin, ctx: Unit): Any {
        val joinType = when (rel.joinType.code()) {
            JoinType.INNER -> PJoinType.INNER
            JoinType.LEFT -> PJoinType.LEFT
            JoinType.RIGHT -> PJoinType.RIGHT
            JoinType.FULL -> PJoinType.FULL
            else -> error("Unsupported join type: ${rel.joinType}")
        }
        return PRel.Join(visitRel(rel.left), visitRel(rel.right), visitRex(rel.condition), joinType, rel.type)
    }

    override fun visitSort(rel: RelSort, ctx: Unit): Any =
        PRel.Sort(visitRel(rel.input), rel.collations.map { toCollation(it) }, rel.type)

    override fun visitDistinct(rel: RelDistinct, ctx: Unit): Any =
        PRel.Distinct(visitRel(rel.input), rel.type)

    override fun visitLimit(rel: RelLimit, ctx: Unit): Any =
        PRel.Limit(visitRel(rel.input), visitRex(rel.limit), rel.type)

    override fun visitOffset(rel: RelOffset, ctx: Unit): Any =
        PRel.Offset(visitRel(rel.input), visitRex(rel.offset), rel.type)

    override fun visitAggregate(rel: RelAggregate, ctx: Unit): Any {
        val input = visitRel(rel.input)
        val groups = rel.groups.map { visitRex(it) }
        val measures = rel.measures.map { m ->
            PMeasure(m.agg, m.args.map { visitRex(it) }, m.isDistinct)
        }
        return PRel.Aggregate(input, measures, groups, rel.type)
    }

    override fun visitUnion(rel: RelUnion, ctx: Unit): Any =
        PRel.Union(visitRel(rel.left), visitRel(rel.right), rel.isAll, rel.type)

    override fun visitIntersect(rel: RelIntersect, ctx: Unit): Any =
        PRel.Intersect(visitRel(rel.left), visitRel(rel.right), rel.isAll, rel.type)

    override fun visitExcept(rel: RelExcept, ctx: Unit): Any =
        PRel.Except(visitRel(rel.left), visitRel(rel.right), rel.isAll, rel.type)

    override fun visitExclude(rel: RelExclude, ctx: Unit): Any =
        PRel.Exclude(visitRel(rel.input), rel.exclusions, rel.type)

    @Suppress("DEPRECATION")
    override fun visitWindow(rel: RelWindow, ctx: Unit): Any {
        val input = visitRel(rel.input)
        val functions = rel.windowFunctions.map { wf ->
            PWindowFn(wf.signature, wf.arguments.map { visitRex(it) })
        }
        val partitions = rel.partitions.map { visitRex(it) }
        val sorts = rel.collations.map { toCollation(it) }
        return PRel.Window(input, functions, partitions, sorts, rel.type)
    }

    override fun visitWith(rel: RelWith, ctx: Unit): Any = visitRel(rel.input)

    // --- Helpers ---

    private fun toCollation(c: Collation): PCollation {
        val expr = visitRex(c.column)
        val desc = c.order.code() == Collation.Order.DESC
        val nullsLast = c.nulls.code() == Collation.Nulls.LAST
        return PCollation(expr, desc, nullsLast)
    }
}
