package org.partiql.plan.operator.rex

/**
 * TODO DOCUMENTATION
 *
 * @param R     Visit return type
 * @param C     Context parameter type
 */
public interface RexVisitor<R, C> {

    public fun defaultVisit(rex: Rex, ctx: C): R {
        for (child in rex.getOperands()) {
            child.accept(this, ctx)
        }
        return defaultReturn(rex, ctx)
    }

    public fun defaultReturn(rex: Rex, ctx: C): R

    public fun visit(rex: Rex, ctx: C): R = rex.accept(this, ctx)

    public fun visitCall(rex: RexCall, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCase(rex: RexCase, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCast(rex: RexCast, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCoalesce(rex: RexCoalesce, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitCollection(rex: RexCollection, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitGlobal(rex: RexGlobal, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitLit(rex: RexLit, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitPath(rex: RexPath, ctx: C): R = rex.accept(this, ctx)

    public fun visitPathIndex(rex: RexPath.Index, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitPathKey(rex: RexPath.Key, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitPathSymbol(rex: RexPath.Symbol, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitPivot(rex: RexPivot, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitSelect(rex: RexSelect, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitStruct(rex: RexStruct, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitSubquery(rex: RexSubquery, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitSubqueryIn(rex: RexSubqueryIn, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitTupleUnion(rex: RexTupleUnion, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitVar(rex: RexVar, ctx: C): R = defaultVisit(rex, ctx)
}
