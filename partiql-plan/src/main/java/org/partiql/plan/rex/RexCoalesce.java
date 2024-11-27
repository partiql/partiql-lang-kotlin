package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical coalesce expression abstract base class.
 */
public abstract class RexCoalesce extends RexBase {

    /**
     * @return the list of arguments (also the children).
     */
    @NotNull
    public abstract List<Rex> getArgs();

    @Override
    protected final RexType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @Override
    protected final List<Operator> children() {
        List<Rex> varargs = getArgs();
        return List.copyOf(varargs);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitCoalesce(this, ctx);
    }
}
