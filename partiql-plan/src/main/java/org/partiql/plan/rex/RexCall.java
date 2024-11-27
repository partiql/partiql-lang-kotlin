package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.spi.function.Function;

import java.util.List;

/**
 * Logical scalar function expression abstract base class.
 */
public abstract class RexCall extends RexBase {

    /**
     * Returns the function to invoke.
     */
    @NotNull
    public abstract Function.Instance getFunction();

    /**
     * Returns the list of function arguments.
     */
    @NotNull
    public abstract List<Rex> getArgs();

    @Override
    protected RexType type() {
        return new RexType(getFunction().returns);
    }

    @Override
    protected List<Operator> children() {
        List<Rex> varargs = getArgs();
        return List.copyOf(varargs);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitCall(this, ctx);
    }
}

