package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.JoinType;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;

import java.util.List;

/**
 * <p>
 * <b>NOTE:</b> This is experimental and subject to change without prior notice!
 * </p>
 * <p>
 * Logical nested-loop joins (correlated subqueries, lateral joins, and cross joins) abstract base class.
 * <pre>
 *     l, r <=> l CROSS JOIN r <=> l JOIN r ON TRUE
 * </pre>
 * </p>
 *
 * {@link RelCorrelate} is modeled off of Calcite's
 * <a href="https://calcite.apache.org/javadocAggregate/org/apache/calcite/rel/core/Correlate.html">Correlate</a>.
 * Calcite's modeling moves the join's ON-condition to the right {@link Rel}'s filter.
 */
public abstract class RelCorrelate extends RelBase {

    /**
     * Creates a new {@link RelCorrelate} instance.
     * @param left left input
     * @param right right input
     * @param joinType join type
     * @return new {@link RelCorrelate} instance
     */
    @NotNull
    public static RelCorrelate create(@NotNull Rel left, @NotNull Rel right, @NotNull JoinType joinType) {
        return new Impl(left, right, joinType);
    }

    /**
     * Gets the left input (operand 0).
     * @return the left input (operand 0)
     */
    @NotNull
    public abstract Rel getLeft();

    /**
     * Gets the right input (operand 1).
     * @return the right input (operand 1)
     */
    @NotNull
    public abstract Rel getRight();

    /**
     * Gets the join type.
     * @return the join type
     */
    @NotNull
    public abstract JoinType getJoinType();

    @NotNull
    @Override
    protected final RelType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getLeft());
        Operand c1 = Operand.single(getRight());
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(@NotNull OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitCorrelate(this, ctx);
    }

    @NotNull
    public abstract RelCorrelate copy(@NotNull Rel left, @NotNull Rel right);

    @NotNull
    public abstract RelCorrelate copy(@NotNull Rel left, @NotNull Rel right, @NotNull JoinType joinType);

    private static class Impl extends RelCorrelate {

        private final Rel left;
        private final Rel right;
        private final JoinType joinType;

        private Impl(Rel left, Rel right, JoinType joinType) {
            this.left = left;
            this.right = right;
            this.joinType = joinType;
        }

        @NotNull
        @Override
        public Rel getLeft() {
            return left;
        }

        @NotNull
        @Override
        public Rel getRight() {
            return right;
        }

        @NotNull
        @Override
        public JoinType getJoinType() {
            return joinType;
        }

        @NotNull
        @Override
        public RelCorrelate copy(@NotNull Rel left, @NotNull Rel right) {
            return new Impl(left, right, joinType);
        }

        @NotNull
        @Override
        public RelCorrelate copy(@NotNull Rel left, @NotNull Rel right, @NotNull JoinType joinType) {
            return new Impl(left, right, joinType);
        }
    }
}
