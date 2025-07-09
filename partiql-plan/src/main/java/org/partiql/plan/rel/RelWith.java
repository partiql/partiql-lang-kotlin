package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.WithListElement;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * <p>
 * <b>NOTE:</b> This is experimental and subject to change without prior notice!
 * </p>
 * <p>
 * WITH relation expression. This is currently experimental and is currently missing some
 * features like recursive CTEs.
 */
public abstract class RelWith extends RelBase {

    /**
     * Creates a new {@link RelWith} instance.
     *
     * @param input input rel (operand 0)
     * @param elements with list elements
     * @return new {@link RelWith} instance
     */
    @NotNull
    public static RelWith create(@NotNull Rel input, @NotNull List<WithListElement> elements) {
        return new Impl(input, elements);
    }

    /**
     * Gets the input rel (operand 0).
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * Gets the WITH list elements.
     * @return WITH list elements.
     */
    @NotNull
    public abstract List<WithListElement> getElements();

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
        return visitor.visitWith(this, ctx);
    }

    @NotNull
    public abstract RelWith copy(@NotNull Rel input);

    @NotNull
    public abstract RelWith copy(@NotNull Rel input, @NotNull List<WithListElement> elements);

    private static class Impl extends RelWith {

        private final Rel input;
        private final List<WithListElement> elements;

        private Impl(Rel input, List<WithListElement> elements) {
            this.input = input;
            this.elements = elements;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public List<WithListElement> getElements() {
            return elements;
        }

        @NotNull
        @Override
        public RelWith copy(@NotNull Rel input) {
            return new Impl(input, elements);
        }

        @NotNull
        @Override
        public RelWith copy(@NotNull Rel input, @NotNull List<WithListElement> elements) {
            return new Impl(input, elements);
        }
    }
}
