package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical variable reference expression abstract base class.
 */
public abstract class RexVar extends RexBase {

    /**
     * @return new variable reference expression.
     */
    @NotNull
    public static RexVar create(int depth, int offset, PType type) {
        return new Impl(depth, offset, type);
    }

    /**
     * @return 0-indexed scope offset.
     */
    public abstract int getDepth();

    /**
     * @return 0-index tuple offset.
     */
    public abstract int getOffset();

    @Override
    protected final List<Operator> operands() {
        return List.of();
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitVar(this, ctx);
    }

    private static class Impl extends RexVar {

        private final int depth;
        private final int offset;
        private final PType type;

        private Impl(int depth, int offset, PType type) {
            this.depth = depth;
            this.offset = offset;
            this.type = type;
        }

        @Override
        protected RexType type() {
            return new RexType(type);
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
