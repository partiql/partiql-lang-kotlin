package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Collation;
import org.partiql.plan.Visitor;

import java.util.Collection;

/**
 * Logical sort operator.
 */
public interface RelSort extends Rel {

    @NotNull
    public Rel getInput();

    @NotNull
    public Collection<Collation> getCollations();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitSort(this, ctx);
    }
}
