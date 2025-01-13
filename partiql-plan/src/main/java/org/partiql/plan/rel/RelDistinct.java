package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;

import java.util.List;

/**
 * Logical distinct operator abstract base class.
 */
public abstract class RelDistinct extends RelBase {

    /**
     * Creates a new {@link RelDistinct} instance.
     *
     * @param input input rel (operand 0)
     * @return new {@link RelDistinct} instance
     */
    @NotNull
    public static RelDistinct create(@NotNull Rel input) {
        return new Impl(input);
    }

    /**
     * Gets the input rel.
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getInput());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitDistinct(this, ctx);
    }

    /**
     * @return copy with new input.
     */
    @NotNull
    public abstract RelDistinct copy(@NotNull Rel input);

    private static class Impl extends RelDistinct {

        private final Rel input;

        private Impl(Rel input) {
            this.input = input;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        /**
         * @return copy with new input (non-final).
         */
        @NotNull
        @Override
        public RelDistinct copy(@NotNull Rel input) {
            return create(input);
        }
    }
}
