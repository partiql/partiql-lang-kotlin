package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

/**
 * Logical `LIMIT` operator.
 */
public interface RelLimit extends Rel {

    @NotNull
    public Rel getInput();

    @NotNull
    public Rex getLimit();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitLimit(this, ctx);
    }
}
