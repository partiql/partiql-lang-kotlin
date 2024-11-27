package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.plan.JoinType;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

/**
 * Logical join operator.
 */
public interface RelJoin extends Rel {

    @NotNull
    public Rel getLeft();

    @NotNull
    public Rel getRight();

    @NotNull
    public JoinType getJoinType();

    @Nullable
    public Rex getCondition();

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitJoin(this, ctx);
    }
}
