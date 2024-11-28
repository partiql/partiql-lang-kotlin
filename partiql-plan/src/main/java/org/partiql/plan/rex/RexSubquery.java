package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rel.Rel;

import java.util.List;

/**
 * Logical subquery expression abstract base class.
 */
public abstract class RexSubquery extends RexBase {

    /**
     * @return new RexSubquery instance
     */
    @NotNull
    public static RexSubquery create(@NotNull Rel input, @NotNull Rex constructor, boolean scalar) {
        return new Impl(input, constructor, scalar);
    }

    /**
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    // TODO REMOVE ME – TEMPORARY UNTIL PLANNER PROPERLY HANDLES SUBQUERIES
    @NotNull
    public abstract Rex getConstructor();

    // TODO REMOVE ME – TEMPORARY UNTIL PLANNER PROPERLY HANDLES SUBQUERIES
    public abstract boolean isScalar();

    @NotNull
    @Override
    protected final RexType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @Override
    protected final List<Operator> children() {
        Rel c0 = getInput();
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitSubquery(this, ctx);
    }

    private static class Impl extends RexSubquery {

        private final Rel input;
        private final Rex constructor;
        private final boolean scalar;

        private Impl(Rel input, Rex constructor, boolean scalar) {
            this.input = input;
            this.constructor = constructor;
            this.scalar = scalar;
        }

        @NotNull
        @Override
        public Rel getInput() {
            return input;
        }

        @NotNull
        @Override
        public Rex getConstructor() {
            return constructor;
        }

        @Override
        public boolean isScalar() {
            return scalar;
        }
    }
}
