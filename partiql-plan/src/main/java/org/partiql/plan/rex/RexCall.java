package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
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
        return RexType.of(getFunction().returns);
    }

    @Override
    protected List<Operand> operands() {
        Operand c0 = Operand.vararg(getArgs());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
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

