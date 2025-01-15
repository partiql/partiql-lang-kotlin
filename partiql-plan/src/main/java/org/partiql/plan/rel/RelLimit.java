package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical limit abstract base class.
 */
public abstract class RelLimit extends RelBase {

    /**
     * Create new {@link RelLimit} instance.
     *
     * @param input input rel (operand 0)
     * @param limit limit rex (operand 1)
     * @return new {@link RelLimit} instance
     */
    @NotNull
    public static RelLimit create(@NotNull Rel input, @NotNull Rex limit) {
        return new Impl(input, limit);
    }

    /**
     * Get input rel (operand 0).
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * Get limit rex (operand 1).
     * @return limit rex (operand 1)
     */
    @NotNull
    public abstract Rex getLimit();

    @NotNull
    @Override
    protected final RelType type() {
        return getInput().getType();
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getInput());
        Operand c1 = Operand.single(getLimit());
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitLimit(this, ctx);
    }

    @NotNull
    public abstract RelLimit copy(@NotNull Rel input);

    @NotNull
    public abstract RelLimit copy(@NotNull Rel input, @NotNull Rex limit);

    private static class Impl extends RelLimit {

        private final Rel input;
        private final Rex limit;

        private Impl(Rel input, Rex limit) {
            this.input = input;
            this.limit = limit;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public Rex getLimit() {
            return limit;
        }

        @NotNull
        @Override
        public RelLimit copy(@NotNull Rel input) {
            return new Impl(input, limit);
        }

        @NotNull
        @Override
        public RelLimit copy(@NotNull Rel input, @NotNull Rex limit) {
            return new Impl(input, limit);
        }
    }
}
