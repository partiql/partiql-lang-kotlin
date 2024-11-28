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

    @NotNull
    public static RexCall create(@NotNull Function.Instance function, @NotNull List<Rex> args) {
        return new Impl(function, args);
    }

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

    private static class Impl extends RexCall {

        private final Function.Instance function;
        private final List<Rex> args;

        private Impl(Function.Instance function, List<Rex> args) {
            this.function = function;
            this.args = args;
        }

        @NotNull
        @Override
        public Function.Instance getFunction() {
            return function;
        }

        @NotNull
        @Override
        public List<Rex> getArgs() {
            return args;
        }
    }
}

