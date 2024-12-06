package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Exclusion;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;

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
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return exclusions (not an operator operand).
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
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getInput());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitExclude(this, ctx);
    }

    @NotNull
    public abstract RelExclude copy(@NotNull Rel input);

    @NotNull
    public abstract RelExclude copy(@NotNull Rel input, @NotNull List<Exclusion> exclusions);

    private static class Impl extends RelExclude {

        private final Rel input;
        private final List<Exclusion> exclusions;

        private Impl(Rel input, List<Exclusion> exclusions) {
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

        @NotNull
        @Override
        public RelExclude copy(@NotNull Rel input) {
            return new Impl(input, exclusions);
        }

        @NotNull
        @Override
        public RelExclude copy(@NotNull Rel input, @NotNull List<Exclusion> exclusions) {
            return new Impl(input, exclusions);
        }
    }
}
