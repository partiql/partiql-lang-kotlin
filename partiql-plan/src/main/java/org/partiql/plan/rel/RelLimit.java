package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical limit abstract base class.
 */
public abstract class RelLimit extends RelBase {

    /**
     * @return new {@link RelLimit} instance
     */
    @NotNull
    public static RelLimit create(@NotNull Rel input, @NotNull Rex limit) {
        return new Impl(input, limit);
    }

    /**
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return limit rex (child 1)
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
    protected final List<Operator> children() {
        Rel c0 = getInput();
        Rex c1 = getLimit();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitLimit(this, ctx);
    }

    private static class Impl extends RelLimit {

        private final Rel input;
        private final Rex limit;

        public Impl(Rel input, Rex limit) {
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
    }
}
