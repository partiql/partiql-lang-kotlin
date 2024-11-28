package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Collation;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical sort abstract base class.
 */
public abstract class RelSort extends RelBase {

    /**
     * @return new {@link RelSort} instance
     */
    @NotNull
    public static RelSort create(@NotNull Rel input, @NotNull List<Collation> collations) {
        return new Impl(input, collations);
    }

    @NotNull
    public abstract Rel getInput();

    @NotNull
    public abstract List<Collation> getCollations();

    @NotNull
    @Override
    protected final RelType type() {
        return getInput().getType();
    }

    @NotNull
    @Override
    protected final List<Operator> children() {
        Rel c0 = getInput();
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitSort(this, ctx);
    }

    private static class Impl extends RelSort {

        private final Rel input;
        private final List<Collation> collations;

        public Impl(Rel input, List<Collation> collations) {
            this.input = input;
            this.collations = collations;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public List<Collation> getCollations() {
            return collations;
        }
    }
}
