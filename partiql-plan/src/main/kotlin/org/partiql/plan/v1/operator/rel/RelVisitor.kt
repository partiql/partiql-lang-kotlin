package org.partiql.plan.v1.operator.rel

/**
 * TODO DOCUMENTATION
 *
 * @param R     Visit return type
 * @param C     Context parameter type
 */
public interface RelVisitor<R, C> {

    public fun defaultVisit(rel: Rel, ctx: C): R {
        for (child in rel.getChildren()) {
            child.accept(this, ctx)
        }
        return defaultReturn(rel, ctx)
    }

    public fun defaultReturn(rel: Rel, ctx: C): R

    public fun visit(rel: Rel, ctx: C): R = rel.accept(this, ctx)

    public fun visitAggregate(rel: RelAggregate, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitDistinct(rel: RelDistinct, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitError(rel: RelError, ctx: C): R = defaultVisit(rel, ctx)

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
}
