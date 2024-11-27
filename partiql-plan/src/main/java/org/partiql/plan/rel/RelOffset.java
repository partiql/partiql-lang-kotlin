package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical offset abstract base class.
 */
public abstract class RelOffset extends RelBase {

    private final RelType type = null;
    private List<Operator> children = null;

    /**
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return offset rex (child 1)
     */
    @NotNull
    public abstract Rex getOffset();

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
            Rex c1 = getOffset();
            children = List.of(c0, c1);
        }
        return children;
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitOffset(this, ctx);
    }
}

