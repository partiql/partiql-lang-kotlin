package org.partiql.plan.v1.rel

/**
 * TODO DOCUMENTATION
 *
 * @param R     Visit return type
 * @param C     Context parameter type
 */
public interface RelVisitor<R, C> {

    public fun defaultVisit(rel: Rel, ctx: C): R {
        for (child in rel.getInputs()) {
            child.accept(this, ctx)
        }
        return defaultReturn(rel, ctx)
    }

    public fun defaultReturn(rel: Rel, ctx: C): R

    public fun visitRel(rel: Rel, ctx: C): R = rel.accept(this, ctx)

    public fun visitRelAggregate(rel: RelAggregate, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelDistinct(rel: RelDistinct, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelExcept(rel: RelExcept, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelExclude(rel: RelExclude, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelFilter(rel: RelFilter, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelIntersect(rel: RelIntersect, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelJoin(rel: RelJoin, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelLimit(rel: RelLimit, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelOffset(rel: RelOffset, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelProject(rel: RelProject, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelScan(rel: RelScan, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelScanIndexed(rel: RelScanIndexed, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelSort(rel: RelSort, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelUnion(rel: RelUnion, ctx: C): R = defaultVisit(rel, ctx)

    public fun visitRelUnpivot(rel: RelUnpivot, ctx: C): R = defaultVisit(rel, ctx)
}
