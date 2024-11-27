package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;

/**
 * Logical `EXCEPT [ALL|DISTINCT]` operator for set (or multiset) difference.
 */
public interface RelExcept extends Rel {

    /**
     * @return true if the `ALL` keyword was used, false otherwise.
     */
    public boolean isAll();

    @NotNull
    public Rel getLeft();

    @NotNull
    public Rel getRight();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitExcept(this, ctx);
    }
}
