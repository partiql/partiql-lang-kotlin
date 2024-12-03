package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical spread expression abstract base class.
 */
public abstract class RexSpread extends RexBase {

    /**
     * @return new RexSpread instance
     */
    @NotNull
    public static RexSpread create(@NotNull List<Rex> args) {
        return new Impl(args);
    }

    /**
     * @return list of spread arguments (the operands)
     */
    public abstract List<Rex> getArgs();

    @NotNull
    @Override
    protected final RexType type() {
        return RexType.of(PType.struct());
    }

    @NotNull
    @Override
    protected final List<Operator> operands() {
        List<Rex> varargs = getArgs().stream().toList();
        return List.copyOf(varargs);
    }

    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitSpread(this, ctx);
    }

    private static class Impl extends RexSpread {

        private final List<Rex> args;

        private Impl(List<Rex> args) {
            this.args = args;
        }

        @Override
        public List<Rex> getArgs() {
            return args;
        }
    }
}
