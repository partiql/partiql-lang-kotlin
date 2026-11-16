package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.errors.PError;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * This represents scenarios in which certain operations are statically known to fail in strict mode but return missing
 * in permissive mode.
 */
public abstract class RexError extends RexBase {

    /**
     * Creates a new instance of RexError.
     * @return new RexError instance
     */
    @NotNull
    public static RexError create() {
        return new Impl(null);
    }

    /**
     * Creates a new instance of RexError that keeps the problem the planner reported, so that an evaluator in strict mode
     * can raise the same error.
     * @param error the problem reported for this expression. A copy is kept, so later changes to {@code error} do not
     *              change this node.
     * @return new RexError instance
     */
    @NotNull
    public static RexError create(@NotNull PError error) {
        return new Impl(error.copy());
    }

    /**
     * Returns the problem the planner reported for this expression, or {@code null} if it was not recorded.
     * @return a new copy of the error on each call, so that callers cannot change this node, which may be shared by
     * cached plans.
     */
    @Nullable
    public PError getError() {
        return null;
    }

    @NotNull
    @Override
    protected RexType type() {
        return RexType.of(PType.unknown());
    }

    @NotNull
    @Override
    protected List<Operand> operands() {
        return List.of();
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitError(this, ctx);
    }

    private static class Impl extends RexError {

        @Nullable
        private final PError error;

        private Impl(@Nullable PError error) {
            this.error = error;
        }

        @Nullable
        @Override
        public PError getError() {
            return error == null ? null : error.copy();
        }
    }
}
