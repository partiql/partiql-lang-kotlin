package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
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
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return projection (not a child, it's a list not an operator).
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
    protected final List<Operator> children() {
        Rel c0 = getInput();
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitProject(this, ctx);
    }

    private static class Impl extends RelProject {

        private final Rel input;
        private final List<Rex> projections;

        public Impl(Rel input, List<Rex> projections) {
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
    }
}
