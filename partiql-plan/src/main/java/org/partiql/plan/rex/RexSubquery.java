package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.plan.rel.Rel;

import java.util.List;

/**
 * Logical subquery expression abstract base class.
 */
public abstract class RexSubquery extends RexBase {

    /**
     * Creates a new RexSubquery instance.
     * @param input input rel (operand 0)
     * @param constructor constructor expression
     * @param scalar true if the subquery is a scalar subquery
     * @return new RexSubquery instance
     */
    @NotNull
    public static RexSubquery create(@NotNull Rel input, @NotNull Rex constructor, boolean scalar) {
        return new Impl(input, constructor, scalar);
    }

    /**
     * @return input rel (operand 0)
     */
    @NotNull
    public abstract Rel getInput();

    // TODO REMOVE ME – TEMPORARY UNTIL PLANNER PROPERLY HANDLES SUBQUERIES
    /**
     * This is experimental and subject to change without prior notice!
     * @return constructor expression
     */
    @NotNull
    public abstract Rex getConstructor();

    // TODO REMOVE ME – TEMPORARY UNTIL PLANNER PROPERLY HANDLES SUBQUERIES
    /**
     * This is experimental and subject to change without prior notice!
     * @return true if the subquery is a scalar subquery
     */
    public abstract boolean isScalar();

    @NotNull
    @Override
    protected final RexType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getInput());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
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
