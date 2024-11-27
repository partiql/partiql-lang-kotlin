package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Visitor;

/**
 * Logical operator for nested-loop joins (correlated subqueries // lateral joins).
 */
public interface RelCorrelate extends Rel {

    @NotNull
    public Rel getLeft();

    @NotNull
    public Rel getRight();

    @NotNull
    public Rel getJoinType();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitCorrelate(this, ctx);
    }
}
