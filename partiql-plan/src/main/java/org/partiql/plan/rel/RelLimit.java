package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical limit abstract base class.
 */
public abstract class RelLimit extends RelBase {

    /**
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return limit rex (child 1)
     */
    @NotNull
    public abstract Rex getLimit();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operator> children() {
        Rel c0 = getInput();
        Rex c1 = getLimit();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitLimit(this, ctx);
    }
}
