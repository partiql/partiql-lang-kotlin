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
     * @return new RexCoalesce instance
     */
    @NotNull
    public static RexCoalesce create(List<Rex> args) {
        return new Impl(args);
    }

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

    private static class Impl extends RexCoalesce {

        private final List<Rex> args;

        private Impl(List<Rex> args) {
            this.args = args;
        }

        @NotNull
        @Override
        public List<Rex> getArgs() {
            return args;
        }
    }
}
