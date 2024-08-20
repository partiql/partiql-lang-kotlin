package org.partiql.eval.internal

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.internal.operator.rel.RelOpAggregate
import org.partiql.eval.internal.operator.rel.RelOpDistinct
import org.partiql.eval.internal.operator.rel.RelOpExceptAll
import org.partiql.eval.internal.operator.rel.RelOpExceptDistinct
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
import org.partiql.eval.internal.operator.rex.ExprLiteral
import org.partiql.eval.internal.operator.rex.ExprPermissive
import org.partiql.eval.value.Datum
import org.partiql.plan.relType
import org.partiql.plan.v1.operator.rel.Rel
import org.partiql.plan.v1.operator.rel.RelAggregate
import org.partiql.plan.v1.operator.rel.RelCollation
import org.partiql.plan.v1.operator.rel.RelDistinct
import org.partiql.plan.v1.operator.rel.RelError
import org.partiql.plan.v1.operator.rel.RelExcept
import org.partiql.plan.v1.operator.rel.RelExclude
import org.partiql.plan.v1.operator.rel.RelFilter
import org.partiql.plan.v1.operator.rel.RelIntersect
import org.partiql.plan.v1.operator.rel.RelIterate
import org.partiql.plan.v1.operator.rel.RelJoin
import org.partiql.plan.v1.operator.rel.RelJoinType
import org.partiql.plan.v1.operator.rel.RelLimit
import org.partiql.plan.v1.operator.rel.RelOffset
import org.partiql.plan.v1.operator.rel.RelProject
import org.partiql.plan.v1.operator.rel.RelScan
import org.partiql.plan.v1.operator.rel.RelSort
import org.partiql.plan.v1.operator.rel.RelUnion
import org.partiql.plan.v1.operator.rel.RelUnpivot
import org.partiql.plan.v1.operator.rel.RelVisitor
import org.partiql.plan.v1.operator.rex.Rex
import org.partiql.plan.v1.operator.rex.RexCall
import org.partiql.plan.v1.operator.rex.RexCase
import org.partiql.plan.v1.operator.rex.RexCast
import org.partiql.plan.v1.operator.rex.RexCoalesce
import org.partiql.plan.v1.operator.rex.RexCollection
import org.partiql.plan.v1.operator.rex.RexError
import org.partiql.plan.v1.operator.rex.RexLit
import org.partiql.plan.v1.operator.rex.RexMissing
import org.partiql.plan.v1.operator.rex.RexPath
import org.partiql.plan.v1.operator.rex.RexPivot
import org.partiql.plan.v1.operator.rex.RexSelect
import org.partiql.plan.v1.operator.rex.RexSpread
import org.partiql.plan.v1.operator.rex.RexStruct
import org.partiql.plan.v1.operator.rex.RexSubquery
import org.partiql.plan.v1.operator.rex.RexSubqueryComp
import org.partiql.plan.v1.operator.rex.RexSubqueryIn
import org.partiql.plan.v1.operator.rex.RexSubqueryTest
import org.partiql.plan.v1.operator.rex.RexTable
import org.partiql.plan.v1.operator.rex.RexVar
import org.partiql.plan.v1.operator.rex.RexVisitor
import org.partiql.planner.catalog.Session

/**
 * The V1 implementation of a
 */
internal class SqlCompiler(
    @JvmField val mode: PartiQLEngine.Mode,
    @JvmField val session: Session,
) {

    private val relCompiler = RelCompiler()

    private val rexCompiler = RexCompiler()

    fun compile(rel: Rel, ctx: Unit): Operator.Relation = rel.accept(relCompiler, ctx)

    fun compile(rex: Rex, ctx: Unit): Operator.Expr = rex.accept(rexCompiler, ctx)

    /**
     * Transforms plan relation operators into the internal physical operators.
     */
    private inner class RelCompiler : RelVisitor<Operator.Relation, Unit> {

        override fun defaultReturn(rel: Rel, ctx: Unit): Operator.Relation {
            TODO("Evaluation is not implemented for rel: ${rel::class.simpleName}")
        }

        override fun visitError(rel: RelError, ctx: Unit): Operator.Relation {
            throw IllegalStateException(rel.message)
        }

        // OPERATORS

        override fun visitAggregate(rel: RelAggregate, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            val keys = rel.getGroups().map { compile(it, ctx).catch() }
            val aggs = rel.getCalls().map { TODO() }
            return RelOpAggregate(input, keys, aggs)
        }

        override fun visitDistinct(rel: RelDistinct, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            return RelOpDistinct(input)
        }

        override fun visitExcept(rel: RelExcept, ctx: Unit): Operator.Relation {
            val lhs = compile(rel.getLeft(), ctx)
            val rhs = compile(rel.getRight(), ctx)
            return when (rel.isAll()) {
                true -> RelOpExceptAll(lhs, rhs)
                else -> RelOpExceptDistinct(lhs, rhs)
            }
        }

        override fun visitExclude(rel: RelExclude, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            TODO("EXCLUDE paths in compiler")
        }

        override fun visitFilter(rel: RelFilter, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            val predicate = compile(rel.getPredicate(), ctx)
            return RelOpFilter(input, predicate)
        }

        override fun visitIntersect(rel: RelIntersect, ctx: Unit): Operator.Relation {
            val lhs = compile(rel.getLeft(), ctx)
            val rhs = compile(rel.getRight(), ctx)
            return when (rel.isAll()) {
                true -> RelOpIntersectAll(lhs, rhs)
                else -> RelOpIntersectDistinct(lhs, rhs)
            }
        }

        override fun visitIterate(rel: RelIterate, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            return when (mode) {
                PartiQLEngine.Mode.PERMISSIVE -> RelOpIteratePermissive(input)
                PartiQLEngine.Mode.STRICT -> RelOpIterate(input)
            }
        }

        override fun visitJoin(rel: RelJoin, ctx: Unit): Operator.Relation {
            val lhs = compile(rel.getLeft(), ctx)
            val rhs = compile(rel.getRight(), ctx)
            val condition = rel.getCondition()?.let { compile(it, ctx) } ?: ExprLiteral(Datum.bool(true))

            // TODO JOIN SCHEMAS
            val lhsType = relType(emptyList(), emptySet())
            val rhsType = relType(emptyList(), emptySet())

            return when (rel.getType()) {
                RelJoinType.INNER -> RelOpJoinInner(lhs, rhs, condition)
                RelJoinType.LEFT -> RelOpJoinOuterLeft(lhs, rhs, condition, rhsType)
                RelJoinType.RIGHT -> RelOpJoinOuterRight(lhs, rhs, condition, lhsType)
                RelJoinType.FULL -> RelOpJoinOuterFull(lhs, rhs, condition, lhsType, rhsType)
                RelJoinType.OTHER -> error("Unknown join type `OTHER`")
            }
        }

        override fun visitLimit(rel: RelLimit, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            val limit = compile(rel.getLimit(), ctx)
            return RelOpLimit(input, limit)
        }

        override fun visitOffset(rel: RelOffset, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            val offset = compile(rel.getOffset(), ctx)
            return RelOpOffset(input, offset)
        }

        override fun visitProject(rel: RelProject, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            val projections = rel.getProjections().map { compile(it, ctx).catch() }
            return RelOpProject(input, projections)
        }

        override fun visitScan(rel: RelScan, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            return when (mode) {
                PartiQLEngine.Mode.PERMISSIVE -> RelOpScanPermissive(input)
                PartiQLEngine.Mode.STRICT -> RelOpScan(input)
            }
        }

        override fun visitSort(rel: RelSort, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            val collations = rel.getCollations().map {
                val expr = compile(it.getRex(), ctx)
                val desc = it.getOrder() == RelCollation.Order.DESC
                val last = it.getNulls() == RelCollation.Nulls.LAST
                RelOpSort.Collation(expr, desc, last)
            }
            return RelOpSort(input, collations)
        }

        override fun visitUnion(rel: RelUnion, ctx: Unit): Operator.Relation {
            val lhs = compile(rel.getLeft(), ctx)
            val rhs = compile(rel.getRight(), ctx)
            return when (rel.isAll()) {
                true -> RelOpUnionAll(lhs, rhs)
                else -> RelOpUnionDistinct(lhs, rhs)
            }
        }

        override fun visitUnpivot(rel: RelUnpivot, ctx: Unit): Operator.Relation {
            val input = compile(rel.getInput(), ctx)
            return when (mode) {
                PartiQLEngine.Mode.PERMISSIVE -> RelOpUnpivot.Permissive(input)
                PartiQLEngine.Mode.STRICT -> RelOpUnpivot.Strict(input)
            }
        }
    }

    /**
     * Transforms plan expression operators into the internal physical expressions.
     */
    private inner class RexCompiler : RexVisitor<Operator.Expr, Unit> {

        override fun defaultVisit(rex: Rex, ctx: Unit): Operator.Expr {
            return super.defaultVisit(rex, ctx)
        }

        override fun defaultReturn(rex: Rex, ctx: Unit): Operator.Expr {
            TODO("Not yet implemented")
        }

        override fun visitCall(rex: RexCall, ctx: Unit): Operator.Expr {
            return super.visitCall(rex, ctx)
        }

        override fun visitCase(rex: RexCase, ctx: Unit): Operator.Expr {
            return super.visitCase(rex, ctx)
        }

        override fun visitCast(rex: RexCast, ctx: Unit): Operator.Expr {
            return super.visitCast(rex, ctx)
        }

        override fun visitCoalesce(rex: RexCoalesce, ctx: Unit): Operator.Expr {
            return super.visitCoalesce(rex, ctx)
        }

        override fun visitCollection(rex: RexCollection, ctx: Unit): Operator.Expr {
            return super.visitCollection(rex, ctx)
        }

        override fun visitError(rex: RexError, ctx: Unit): Operator.Expr {
            return super.visitError(rex, ctx)
        }

        override fun visitLit(rex: RexLit, ctx: Unit): Operator.Expr {
            return super.visitLit(rex, ctx)
        }

        override fun visitMissing(rex: RexMissing, ctx: Unit): Operator.Expr {
            return super.visitMissing(rex, ctx)
        }

        override fun visitPath(rex: RexPath, ctx: Unit): Operator.Expr {
            return super.visitPath(rex, ctx)
        }

        override fun visitPathIndex(rex: RexPath.Index, ctx: Unit): Operator.Expr {
            return super.visitPathIndex(rex, ctx)
        }

        override fun visitPathKey(rex: RexPath.Key, ctx: Unit): Operator.Expr {
            return super.visitPathKey(rex, ctx)
        }

        override fun visitPathSymbol(rex: RexPath.Symbol, ctx: Unit): Operator.Expr {
            return super.visitPathSymbol(rex, ctx)
        }

        override fun visitPivot(rex: RexPivot, ctx: Unit): Operator.Expr {
            return super.visitPivot(rex, ctx)
        }

        override fun visitSelect(rex: RexSelect, ctx: Unit): Operator.Expr {
            return super.visitSelect(rex, ctx)
        }

        override fun visitStruct(rex: RexStruct, ctx: Unit): Operator.Expr {
            return super.visitStruct(rex, ctx)
        }

        override fun visitSubquery(rex: RexSubquery, ctx: Unit): Operator.Expr {
            return super.visitSubquery(rex, ctx)
        }

        override fun visitSubqueryComp(rex: RexSubqueryComp, ctx: Unit): Operator.Expr {
            return super.visitSubqueryComp(rex, ctx)
        }

        override fun visitSubqueryIn(rex: RexSubqueryIn, ctx: Unit): Operator.Expr {
            return super.visitSubqueryIn(rex, ctx)
        }

        override fun visitSubqueryTest(rex: RexSubqueryTest, ctx: Unit): Operator.Expr {
            return super.visitSubqueryTest(rex, ctx)
        }

        override fun visitSpread(rex: RexSpread, ctx: Unit): Operator.Expr {
            return super.visitSpread(rex, ctx)
        }

        override fun visitTable(rex: RexTable, ctx: Unit): Operator.Expr {
            return super.visitTable(rex, ctx)
        }

        override fun visitVar(rex: RexVar, ctx: Unit): Operator.Expr {
            return super.visitVar(rex, ctx)
        }
    }

    /**
     * Some places "catch" an error and return the MISSING value.
     */
    private fun Operator.Expr.catch(): Operator.Expr = when (mode) {
        PartiQLEngine.Mode.PERMISSIVE -> ExprPermissive(this)
        PartiQLEngine.Mode.STRICT -> this
    }
}