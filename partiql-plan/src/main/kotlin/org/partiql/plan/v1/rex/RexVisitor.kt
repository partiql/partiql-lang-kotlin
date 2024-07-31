package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 *
 * @param R     Visit return type
 * @param C     Context parameter type
 */
public interface RexVisitor<R, C> {

    public fun defaultVisit(rex: Rex, ctx: C): R {
        for (child in rex.getInputs()) {
            child.accept(this, ctx)
        }
        return defaultReturn(rex, ctx)
    }

    public fun defaultReturn(rex: Rex, ctx: C): R

    public fun visitRex(rex: Rex, ctx: C): R = rex.accept(this, ctx)

    public fun visitRexCall(rex: RexCall, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexCase(rex: RexCase, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexCast(rex: RexCast, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexCoalesce(rex: RexCoalesce, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexCollection(rex: RexCollection, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexGlobal(rex: RexGlobal, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexLit(rex: RexLit, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexNullIf(rex: RexNullIf, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexPath(rex: RexPath, ctx: C): R = rex.accept(this, ctx)

    public fun visitRexPathIndex(rex: RexPath.Index, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexPathKey(rex: RexPath.Key, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexPathSymbol(rex: RexPath.Symbol, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexPivot(rex: RexPivot, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexSelect(rex: RexSelect, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexStruct(rex: RexStruct, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexSubquery(rex: RexSubquery, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexSubqueryIn(rex: RexSubqueryIn, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexTupleUnion(rex: RexTupleUnion, ctx: C): R = defaultVisit(rex, ctx)

    public fun visitRexVar(rex: RexVar, ctx: C): R = defaultVisit(rex, ctx)
}
