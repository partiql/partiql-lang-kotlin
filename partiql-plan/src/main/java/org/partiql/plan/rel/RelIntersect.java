package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical intersect abstract base class.
 */
public abstract class RelIntersect extends RelBase {

    private final RelType type = null;
    private List<Operator> children = null;

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
    public final RelType getType() {
        if (type == null) {
            throw new UnsupportedOperationException("Derive type is not implemented");
        }
        return type;
    }

    @NotNull
    @Override
    public final List<Operator> getChildren() {
        if (children == null) {
            Rel c0 = getLeft();
            Rel c1 = getRight();
            children = List.of(c0, c1);
        }
        return children;
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitIntersect(this, ctx);
    }
}
