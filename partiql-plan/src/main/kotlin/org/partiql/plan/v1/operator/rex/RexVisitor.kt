package org.partiql.plan.v1.operator.rex

/**
 * TODO DOCUMENTATION
 *
 * @param R     Visit return type
 * @param C     Context parameter type
 */
public interface RexVisitor<R, C> {

    public fun defaultVisit(rex: Rex, ctx: C): R {
        for (child in rex.getChildren()) {
            child.accept(this, ctx)
        }
        return defaultReturn(rex, ctx)
    }

    public fun defaultReturn(rex: Rex, ctx: C): R

    public fun visit(rex: Rex, ctx: C): R = rex.accept(this, ctx)

    public fun visitArray(rex: RexArray, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitBag(rex: RexBag, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCall(rex: RexCall, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCallDynamic(rex: RexCallDynamic, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCase(rex: RexCase, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCast(rex: RexCast, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCoalesce(rex: RexCoalesce, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitError(rex: RexError, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitLit(rex: RexLit, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitMissing(rex: RexMissing, ctx: C): R = defaultVisit(rex, ctx)

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
