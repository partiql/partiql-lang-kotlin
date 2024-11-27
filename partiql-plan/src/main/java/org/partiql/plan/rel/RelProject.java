package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

import java.util.Collection;
import java.util.List;

/**
 * Logical project abstract base class.
 */
public abstract class RelProject extends RelBase {

    private final RelType type = null;
    private List<Operator> children = null;

    /**
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return projection (not a child, it's a list not an operator).
     */
    @NotNull
    public abstract Collection<Rex> getProjections();

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
            Rel c0 = getInput();
            children = List.of(c0);
        }
        return children;
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitProject(this, ctx);
    }
}
