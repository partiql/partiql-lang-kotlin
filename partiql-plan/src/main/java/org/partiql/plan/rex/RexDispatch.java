package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.spi.function.Function;

import java.util.List;

/**
 * Logical operator for a dynamic dispatch.
 */
public abstract class RexDispatch extends RexBase {

    /**
     * Dynamic function name.
     */
    public abstract String getName();

    /**
     * Returns the functions to dispatch to.
     */
    public abstract List<Function> getFunctions();

    /**
     * Returns the list of function arguments.
     */
    public abstract List<Rex> getArgs();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.dynamic();
    }

    @Override
    protected final List<Operator> children() {
        List<Rex> varargs = getArgs();
        return List.copyOf(varargs);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitCallDynamic(this, ctx);
    }
}
