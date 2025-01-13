package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Collation;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;

import java.util.List;

/**
 * Logical sort abstract base class.
 */
public abstract class RelSort extends RelBase {

    /**
     * Creates a new {@link RelSort} instance.
     *
     * @param input the input to sort
     * @param collations the collations to sort by
     * @return new {@link RelSort} instance
     */
    @NotNull
    public static RelSort create(@NotNull Rel input, @NotNull List<Collation> collations) {
        return new Impl(input, collations);
    }

    /**
     * Gets the input to sort (operand 0).
     * @return the input to sort (operand 0).
     */
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
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getInput());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitSort(this, ctx);
    }

    @NotNull
    public abstract RelSort copy(@NotNull Rel input);

    @NotNull
    public abstract RelSort copy(@NotNull Rel input, @NotNull List<Collation> collations);

    private static class Impl extends RelSort {

        private final Rel input;
        private final List<Collation> collations;

        private Impl(Rel input, List<Collation> collations) {
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

        @NotNull
        @Override
        public RelSort copy(@NotNull Rel input) {
            return new Impl(input, collations);
        }

        @NotNull
        @Override
        public RelSort copy(@NotNull Rel input, @NotNull List<Collation> collations) {
            return new Impl(input, collations);
        }
    }
}
