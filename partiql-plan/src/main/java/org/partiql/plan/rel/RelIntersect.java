package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;

/**
 * Logical `INTERSECT [ALL|DISTINCT]` operator for set (or multiset) intersection.
 */
public interface RelIntersect extends Rel {

    public boolean isAll();

    @NotNull
    public Rel getLeft();

    @NotNull
    public Rel getRight();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitIntersect(this, ctx);
    }
}
