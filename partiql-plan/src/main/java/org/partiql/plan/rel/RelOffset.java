package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
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
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return offset rex (child 1)
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
    protected final List<Operator> children() {
        Rel c0 = getInput();
        Rex c1 = getOffset();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitOffset(this, ctx);
    }

    private static class Impl extends RelOffset {

        private final Rel input;
        private final Rex offset;

        public Impl(Rel input, Rex offset) {
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
    }
}
