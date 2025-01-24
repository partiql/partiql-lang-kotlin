package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.function.Fn;

import java.util.List;

/**
 * Logical scalar function expression abstract base class.
 */
public abstract class RexCall extends RexBase {

    /**
     * Creates a new scalar function expression.
     * @param function the function instance backing the call
     * @param args the arguments to the function
     * @return a new scalar function expression
     */
    @NotNull
    public static RexCall create(@NotNull Fn function, @NotNull List<Rex> args) {
        return new Impl(function, args);
    }

    /**
     * Returns the function to invoke.
     *
     * @return the function to invoke
     */
    @NotNull
    public abstract Fn getFunction();

    /**
     * Returns the list of function arguments.
     * @return the list of function arguments
     */
    @NotNull
    public abstract List<Rex> getArgs();

    @NotNull
    @Override
    protected RexType type() {
        return RexType.of(getFunction().getSignature().getReturns());
    }

    @NotNull
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

        private final Fn function;
        private final List<Rex> args;

        private Impl(Fn function, List<Rex> args) {
            this.function = function;
            this.args = args;
        }

        @NotNull
        @Override
        public Fn getFunction() {
            return function;
        }

        @NotNull
        @Override
        public List<Rex> getArgs() {
            return args;
        }
    }
}

