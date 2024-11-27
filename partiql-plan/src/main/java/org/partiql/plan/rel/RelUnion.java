package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;

/**
 * Logical `UNION [ALL|DISTINCT]` operator for set (or multiset) union.
 */
public interface RelUnion extends Rel {

    public boolean isAll();

    @NotNull
    public Rel getLeft();

    @NotNull
    public Rel getRight();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitUnion(this, ctx);
    }
}

