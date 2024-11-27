package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

/**
 * Logical `OFFSET` operator.
 */
public interface RelOffset extends Rel {

    @NotNull
    public Rel getInput();

    @NotNull
    public Rex getOffset();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitOffset(this, ctx);
    }
}

