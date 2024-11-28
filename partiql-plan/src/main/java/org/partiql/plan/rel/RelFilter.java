package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical filter abstract base class.
 */
public abstract class RelFilter extends RelBase {

    /**
     * @return new {@link RelFilter} instance
     */
    @NotNull
    public static RelFilter create(@NotNull Rel input, @NotNull Rex predicate) {
        return new Impl(input, predicate);
    }

    /**
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return predicate rex (child 1)
     */
    @NotNull
    public abstract Rex getPredicate();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operator> children() {
        Rel c0 = getInput();
        Rex c1 = getPredicate();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitFilter(this, ctx);
    }

    private static class Impl extends RelFilter {

        private final Rel input;
        private final Rex predicate;

        public Impl(Rel input, Rex predicate) {
            this.input = input;
            this.predicate = predicate;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public Rex getPredicate() {
            return predicate;
        }
    }
}
