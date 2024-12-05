package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
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
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return predicate rex.
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
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getInput());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitFilter(this, ctx);
    }

    @NotNull
    public abstract RelFilter copy(@NotNull Rel input);

    @NotNull
    public abstract RelFilter copy(@NotNull Rel input, @NotNull Rex predicate);

    private static class Impl extends RelFilter {

        private final Rel input;
        private final Rex predicate;

        private Impl(Rel input, Rex predicate) {
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

        @NotNull
        @Override
        public RelFilter copy(@NotNull Rel input) {
            return new Impl(input, predicate);
        }

        @NotNull
        @Override
        public RelFilter copy(@NotNull Rel input, @NotNull Rex predicate) {
            return new Impl(input, predicate);
        }
    }
}
