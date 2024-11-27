package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical union abstract base class.
 */
public abstract class RelUnion extends RelBase {

    /**
     * @return true if ALL else DISTINCT.
     */
    public abstract boolean isAll();

    /**
     * @return left rel (child 0)
     */
    @NotNull
    public abstract Rel getLeft();

    /**
     * @return right rel (child 1)
     */
    @NotNull
    public abstract Rel getRight();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operator> children() {
        Rel c0 = getLeft();
        Rel c1 = getRight();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitUnion(this, ctx);
    }
}
