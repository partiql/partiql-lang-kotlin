package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Exclusion;
import org.partiql.plan.Visitor;

import java.util.Collection;

/**
 * Logical `EXCLUDE` operator.
 */
public interface RelExclude extends Rel {

    @NotNull
    public Rel getInput();

    @NotNull
    public Collection<Exclusion> getExclusions();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitExclude(this, ctx);
    }
}
