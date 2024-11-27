package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

/**
 * Logical `UNPIVOT` operator.
 */
public interface RelUnpivot extends Rel {

    @NotNull
    public Rex getRex();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitUnpivot(this, ctx);
    }
}
