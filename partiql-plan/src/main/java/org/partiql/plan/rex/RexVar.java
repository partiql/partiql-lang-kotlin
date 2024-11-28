package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical variable reference expression abstract base class.
 */
public abstract class RexVar extends RexBase {

    /**
     * @return new variable reference expression.
     */
    @NotNull
    public static RexVar create(int depth, int offset) {
        return new Impl(depth, offset);
    }

    /**
     * @return 0-indexed scope offset.
     */
    public abstract int getDepth();

    /**
     * @return 0-index tuple offset.
     */
    public abstract int getOffset();

    @NotNull
    @Override
    protected final RexType type() {
        // would need to lookup in context
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @Override
    protected final List<Operator> children() {
        return List.of();
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitVar(this, ctx);
    }

    private static class Impl extends RexVar {

        private final int depth;
        private final int offset;

        private Impl(int depth, int offset) {
            this.depth = depth;
            this.offset = offset;
        }

        @Override
        public int getDepth() {
            return depth;
        }

        @Override
        public int getOffset() {
            return offset;
        }
    }
}
