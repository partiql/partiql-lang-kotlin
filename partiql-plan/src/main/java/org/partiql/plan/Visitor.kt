package org.partiql.plan

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
import org.partiql.plan.rex.RexDispatch
import org.partiql.plan.rex.RexCase
import org.partiql.plan.rex.RexCast
import org.partiql.plan.rex.RexCoalesce
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
 * A visitor for a logical [Operator] tree.
 *
 * @param R Visit return type
 * @param C Context parameter type
 */
public interface Visitor<R, C> {

    public fun defaultVisit(operator: Operator, ctx: C): R {
        for (child in operator.getChildren()) {
            child.accept(this, ctx)
        }
        return defaultReturn(operator, ctx)
    }

    public fun defaultReturn(operator: Operator, ctx: C): R

    public fun visit(operator: Operator, ctx: C): R = operator.accept(this, ctx)

    // --[Rel]-----------------------------------------------------------------------------------------------------------

    public fun visitAggregate(rel: RelAggregate, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitDistinct(rel: RelDistinct, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitExcept(rel: RelExcept, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitExclude(rel: RelExclude, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitFilter(rel: RelFilter, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitIntersect(rel: RelIntersect, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitIterate(rel: RelIterate, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitJoin(rel: RelJoin, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitCorrelate(rel: RelCorrelate, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitLimit(rel: RelLimit, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitOffset(rel: RelOffset, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitProject(rel: RelProject, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitScan(rel: RelScan, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitSort(rel: RelSort, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitUnion(rel: RelUnion, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitUnpivot(rel: RelUnpivot, ctx: C): R = defaultVisit(rel, ctx)

    // --[Rex]-----------------------------------------------------------------------------------------------------------

    public fun visitArray(rex: RexArray, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitBag(rex: RexBag, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCall(rex: RexCall, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCallDynamic(rex: RexDispatch, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCase(rex: RexCase, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCast(rex: RexCast, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCoalesce(rex: RexCoalesce, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitError(rex: RexError, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitLit(rex: RexLit, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitNullIf(rex: RexNullIf, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitPathIndex(rex: RexPathIndex, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitPathKey(rex: RexPathKey, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitPathSymbol(rex: RexPathSymbol, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitPivot(rex: RexPivot, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitSelect(rex: RexSelect, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitStruct(rex: RexStruct, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitSubquery(rex: RexSubquery, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitSubqueryComp(rex: RexSubqueryComp, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitSubqueryIn(rex: RexSubqueryIn, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitSubqueryTest(rex: RexSubqueryTest, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitSpread(rex: RexSpread, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitTable(rex: RexTable, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitVar(rex: RexVar, ctx: C): R = defaultVisit(rex, ctx)
}
