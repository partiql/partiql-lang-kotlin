package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;

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
     * @return the list of arguments (also the operands).
     */
    @NotNull
    public abstract List<Rex> getArgs();

    @NotNull
    @Override
    protected final RexType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.vararg(getArgs());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
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
