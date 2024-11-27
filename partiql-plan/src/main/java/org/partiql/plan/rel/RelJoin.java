package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.plan.JoinType;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical join abstract base class.
 */
public abstract class RelJoin extends RelBase {

    private final RelType type = null;
    private List<Operator> children = null;

    /**
     * @return left input (child 0)
     */
    @NotNull
    public abstract Rel getLeft();

    /**
     * @return right input (child 1)
     */
    @NotNull
    public abstract Rel getRight();

    /**
     * @return the join condition (child 2), or null if there is no condition.
     */
    @Nullable
    public abstract Rex getCondition();

    /**
     * @return JoinType
     */
    @NotNull
    public abstract JoinType getJoinType();

    @NotNull
    @Override
    public final List<Operator> getChildren() {
        if (children == null) {
            Rel c0 = getLeft();
            Rel c1 = getRight();
            Rex c2 = getCondition(); // can be null!
            children = List.of(c0, c1, c2);
        }
        return children;
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitJoin(this, ctx);
    }
}
