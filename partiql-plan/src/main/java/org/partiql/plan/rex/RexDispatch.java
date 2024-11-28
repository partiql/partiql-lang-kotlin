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
     * @return new RexDispatch instance
     */
    @NotNull
    public static RexDispatch create(String name, List<Function> functions, List<Rex> args) {
        return new Impl(name, functions, args);
    }

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

    private static class Impl extends RexDispatch {

        private final String name;
        private final List<Function> functions;
        private final List<Rex> args;

        private Impl(String name, List<Function> functions, List<Rex> args) {
            this.name = name;
            this.functions = functions;
            this.args = args;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<Function> getFunctions() {
            return functions;
        }

        @Override
        public List<Rex> getArgs() {
            return args;
        }
    }
}
