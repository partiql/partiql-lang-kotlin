package org.partiql.planner

import org.partiql.plan.Action
import org.partiql.plan.Operator
import org.partiql.plan.OperatorVisitor
import org.partiql.plan.Plan
import org.partiql.plan.rel.RelAggregate
import org.partiql.plan.rel.RelCorrelate
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
import org.partiql.plan.rex.RexSubqueryComp
import org.partiql.plan.rex.RexSubqueryIn
import org.partiql.plan.rex.RexSubqueryTest
import org.partiql.plan.rex.RexTable
import org.partiql.plan.rex.RexVar

/**
 * This class asserts the equivalence of two query plans.
 *
 * Replacement for https://github.com/partiql/partiql-lang-kotlin/blob/main/partiql-planner/src/test/kotlin/org/partiql/planner/util/PlanNodeEquivalentVisitor.kt#L16
 */
object PlanEquivalenceOperatorVisitor : OperatorVisitor<Boolean, Any> {

    @JvmStatic
    public fun equals(p1: Plan, p2: Plan): Boolean = visitPlan(p1, p2)

    public fun visitPlan(plan: Plan, other: Any): Boolean {
        if (other !is Plan) {
            return false
        }
        val op1 = plan.action
        val op2 = other.action
        return visitOperation(op1, op2)
    }

    public fun visitOperation(action: Action, other: Any): Boolean {
        if (other !is Action) {
            return false
        }
        if (action is Action.Query && other is Action.Query) {
            // TODO
            return true
        }
        // can only compare query
        return false
    }

    override fun defaultReturn(operator: Operator, other: Any): Boolean = false

    override fun visitAggregate(rel: RelAggregate, other: Any): Boolean {
        return super.visitAggregate(rel, other)
    }

    override fun visitDistinct(rel: RelDistinct, other: Any): Boolean {
        return super.visitDistinct(rel, other)
    }

    override fun visitExcept(rel: RelExcept, other: Any): Boolean {
        return super.visitExcept(rel, other)
    }

    override fun visitExclude(rel: RelExclude, other: Any): Boolean {
        return super.visitExclude(rel, other)
    }

    override fun visitFilter(rel: RelFilter, other: Any): Boolean {
        return super.visitFilter(rel, other)
    }

    override fun visitIntersect(rel: RelIntersect, other: Any): Boolean {
        return super.visitIntersect(rel, other)
    }

    override fun visitIterate(rel: RelIterate, other: Any): Boolean {
        return super.visitIterate(rel, other)
    }

    override fun visitJoin(rel: RelJoin, other: Any): Boolean {
        return super.visitJoin(rel, other)
    }

    override fun visitCorrelate(rel: RelCorrelate, other: Any): Boolean {
        return super.visitCorrelate(rel, other)
    }

    override fun visitLimit(rel: RelLimit, other: Any): Boolean {
        return super.visitLimit(rel, other)
    }

    override fun visitOffset(rel: RelOffset, other: Any): Boolean {
        return super.visitOffset(rel, other)
    }

    override fun visitProject(rel: RelProject, other: Any): Boolean {
        return super.visitProject(rel, other)
    }

    override fun visitScan(rel: RelScan, other: Any): Boolean {
        return super.visitScan(rel, other)
    }

    override fun visitSort(rel: RelSort, other: Any): Boolean {
        return super.visitSort(rel, other)
    }

    override fun visitUnion(rel: RelUnion, other: Any): Boolean {
        return super.visitUnion(rel, other)
    }

    override fun visitUnpivot(rel: RelUnpivot, other: Any): Boolean {
        return super.visitUnpivot(rel, other)
    }

    override fun visitArray(rex: RexArray, other: Any): Boolean {
        return super.visitArray(rex, other)
    }

    override fun visitBag(rex: RexBag, other: Any): Boolean {
        return super.visitBag(rex, other)
    }

    override fun visitCall(rex: RexCall, other: Any): Boolean {
        return super.visitCall(rex, other)
    }

    override fun visitDispatch(rex: RexDispatch, other: Any): Boolean {
        return super.visitDispatch(rex, other)
    }

    override fun visitCase(rex: RexCase, other: Any): Boolean {
        return super.visitCase(rex, other)
    }

    override fun visitCast(rex: RexCast, other: Any): Boolean {
        return super.visitCast(rex, other)
    }

    override fun visitCoalesce(rex: RexCoalesce, other: Any): Boolean {
        return super.visitCoalesce(rex, other)
    }

    override fun visitError(rex: RexError, other: Any): Boolean {
        TODO("visit error")
    }

    override fun visitLit(rex: RexLit, other: Any): Boolean {
        return super.visitLit(rex, other)
    }

    override fun visitNullIf(rex: RexNullIf, other: Any): Boolean {
        return super.visitNullIf(rex, other)
    }

    override fun visitPathIndex(rex: RexPathIndex, other: Any): Boolean {
        return super.visitPathIndex(rex, other)
    }

    override fun visitPathKey(rex: RexPathKey, other: Any): Boolean {
        return super.visitPathKey(rex, other)
    }

    override fun visitPathSymbol(rex: RexPathSymbol, other: Any): Boolean {
        return super.visitPathSymbol(rex, other)
    }

    override fun visitPivot(rex: RexPivot, other: Any): Boolean {
        return super.visitPivot(rex, other)
    }

    override fun visitSelect(rex: RexSelect, other: Any): Boolean {
        return super.visitSelect(rex, other)
    }

    override fun visitStruct(rex: RexStruct, other: Any): Boolean {
        return super.visitStruct(rex, other)
    }

    override fun visitSubquery(rex: RexSubquery, other: Any): Boolean {
        return super.visitSubquery(rex, other)
    }

    override fun visitSubqueryComp(rex: RexSubqueryComp, other: Any): Boolean {
        return super.visitSubqueryComp(rex, other)
    }

    override fun visitSubqueryIn(rex: RexSubqueryIn, other: Any): Boolean {
        return super.visitSubqueryIn(rex, other)
    }

    override fun visitSubqueryTest(rex: RexSubqueryTest, other: Any): Boolean {
        return super.visitSubqueryTest(rex, other)
    }

    override fun visitSpread(rex: RexSpread, other: Any): Boolean {
        return super.visitSpread(rex, other)
    }

    override fun visitTable(rex: RexTable, other: Any): Boolean {
        return super.visitTable(rex, other)
    }

    override fun visitVar(rex: RexVar, other: Any): Boolean {
        return super.visitVar(rex, other)
    }
}
