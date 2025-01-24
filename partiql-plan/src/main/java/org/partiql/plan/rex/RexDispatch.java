package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.function.FnOverload;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * Logical operator for a dynamic dispatch.
 */
public abstract class RexDispatch extends RexBase {

    /**
     * Creates a new RexDispatch instance.
     * @param name dynamic function name
     * @param functions functions to dispatch to
     * @param args function arguments
     * @return new RexDispatch instance
     */
    @NotNull
    public static RexDispatch create(String name, List<FnOverload> functions, List<Rex> args) {
        return new Impl(name, functions, args);
    }

    /**
     * Dynamic function name.
     * @return dynamic function name
     */
    public abstract String getName();

    /**
     * Returns the functions to dispatch to.
     * @return functions to dispatch to
     */
    public abstract List<FnOverload> getFunctions();

    /**
     * Returns the list of function arguments.
     * @return function arguments
     */
    public abstract List<Rex> getArgs();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(PType.dynamic());
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.vararg(getArgs());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitDispatch(this, ctx);
    }

    private static class Impl extends RexDispatch {

        private final String name;
        private final List<FnOverload> functions;
        private final List<Rex> args;

        private Impl(String name, List<FnOverload> functions, List<Rex> args) {
            this.name = name;
            this.functions = functions;
            this.args = args;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<FnOverload> getFunctions() {
            return functions;
        }

        @Override
        public List<Rex> getArgs() {
            return args;
        }
    }
}
