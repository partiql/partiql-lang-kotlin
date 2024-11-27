package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

import java.util.Collection;

/**
 * Logical `PROJECTION` operator
 */
public interface RelProject extends Rel {

    @NotNull
    public Rel getInput();

    @NotNull
    public Collection<Rex> getProjections();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitProject(this, ctx);
    }
}
