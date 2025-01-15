package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;
import org.partiql.spi.types.PType;

import java.util.List;

/**
 * Logical variable reference expression abstract base class.
 */
public abstract class RexVar extends RexBase {

    /**
     * Creates a new variable reference expression.
     * @param scope 0-indexed scope offset.
     * @param offset 0-indexed tuple offset.
     * @return new variable reference expression.
     */
    @NotNull
    public static RexVar create(int scope, int offset, PType type) {
        return new Impl(scope, offset, type);
    }

    /**
     * Returns the scope number of the variable reference.
     * @return 0-indexed scope offset.
     */
    public abstract int getScope();

    /**
     * Returns the offset of the variable reference within the scope.
     * @return 0-index tuple offset.
     */
    public abstract int getOffset();

    @NotNull
    @Override
    protected final List<Operand> operands() {
        return List.of();
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitVar(this, ctx);
    }

    private static class Impl extends RexVar {

        private final int scope;
        private final int offset;
        private final PType type;

        private Impl(int scope, int offset, PType type) {
            this.scope = scope;
            this.offset = offset;
            this.type = type;
        }

        @NotNull
        @Override
        protected RexType type() {
            return RexType.of(type);
        }

        @Override
        public int getScope() {
            return scope;
        }

        @Override
        public int getOffset() {
            return offset;
        }
    }
}
