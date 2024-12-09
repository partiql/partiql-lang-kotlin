package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical offset abstract base class.
 */
public abstract class RelOffset extends RelBase {

    /**
     * @return new {@link RelOffset} instance
     */
    @NotNull
    public static RelOffset create(@NotNull Rel input, @NotNull Rex offset) {
        return new Impl(input, offset);
    }

    /**
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return offset rex (operand 1)
     */
    @NotNull
    public abstract Rex getOffset();

    @NotNull
    @Override
    protected final RelType type() {
        return getInput().getType();
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getInput());
        Operand c1 = Operand.single(getOffset());
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitOffset(this, ctx);
    }

    @NotNull
    public abstract RelOffset copy(@NotNull Rel input);

    @NotNull
    public abstract RelOffset copy(@NotNull Rel input, @NotNull Rex offset);

    private static class Impl extends RelOffset {

        private final Rel input;
        private final Rex offset;

        private Impl(Rel input, Rex offset) {
            this.input = input;
            this.offset = offset;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public Rex getOffset() {
            return offset;
        }

        @NotNull
        @Override
        public RelOffset copy(@NotNull Rel input) {
            return new Impl(input, offset);
        }

        @NotNull
        @Override
        public RelOffset copy(@NotNull Rel input, @NotNull Rex offset) {
            return new Impl(input, offset);
        }
    }
}
