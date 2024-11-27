package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical unpivot abstract base class.
 */
public abstract class RelUnpivot extends RelBase {

    private final RelType type = null;
    private List<Operator> children = null;

    /**
     * @return input rex (child 0)
     */
    @NotNull
    public abstract Rex getRex();

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
            Rex c0 = getRex();
            children = List.of(c0);
        }
        return children;
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitUnpivot(this, ctx);
    }
}
