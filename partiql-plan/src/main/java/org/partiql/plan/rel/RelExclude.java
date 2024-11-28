package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Exclusion;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical exclude abstract base class.
 */
public abstract class RelExclude extends RelBase {

    /**
     * @return new {@link RelExclude} instance
     */
    @NotNull
    public static RelExclude create(@NotNull Rel input, @NotNull List<Exclusion> exclusions) {
        return new Impl(input, exclusions);
    }

    /**
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return exclusions (not an operator child).
     */
    @NotNull
    public abstract List<Exclusion> getExclusions();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operator> children() {
        Rel c0 = getInput();
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitExclude(this, ctx);
    }

    private static class Impl extends RelExclude {

        private final Rel input;
        private final List<Exclusion> exclusions;

        public Impl(Rel input, List<Exclusion> exclusions) {
            this.input = input;
            this.exclusions = exclusions;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public List<Exclusion> getExclusions() {
            return exclusions;
        }
    }

}
