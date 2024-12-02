package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * Logical project abstract base class.
 */
public abstract class RelProject extends RelBase {

    /**
     * @return new {@link RelProject} instance
     */
    @NotNull
    public static RelProject create(Rel input, List<Rex> projections) {
        return new Impl(input, projections);
    }

    /**
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return projection (not a operand, it's a list not an operator).
     */
    @NotNull
    public abstract List<Rex> getProjections();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operator> operands() {
        Rel c0 = getInput();
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitProject(this, ctx);
    }

    @NotNull
    public abstract RelProject copy(@NotNull Rel input);

    @NotNull
    public abstract RelProject copy(@NotNull Rel input, @NotNull List<Rex> projections);

    private static class Impl extends RelProject {

        private final Rel input;
        private final List<Rex> projections;

        private Impl(Rel input, List<Rex> projections) {
            this.input = input;
            this.projections = projections;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public List<Rex> getProjections() {
            return projections;
        }

        @NotNull
        @Override
        public RelProject copy(@NotNull Rel input) {
            return new Impl(input, projections);
        }

        @NotNull
        @Override
        public RelProject copy(@NotNull Rel input, @NotNull List<Rex> projections) {
            return new Impl(input, projections);
        }
    }
}
