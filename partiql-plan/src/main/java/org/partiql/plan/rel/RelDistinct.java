package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;

/**
 * Logical `DISTINCT` operator.
 */
public interface RelDistinct extends Rel {

    @NotNull
    public Rel getInput();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitDistinct(this, ctx);
    }
}
