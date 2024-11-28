package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical path by key lookup expression abstract base class.
 */
public abstract class RexPathKey extends RexBase {

    /**
     * @return new RexPathKey instance
     */
    @NotNull
    public static RexPathKey create(@NotNull Rex operand, @NotNull Rex key) {
        return new Impl(operand, key);
    }

    /**
     * @return operand rex (child 0)
     */
    @NotNull
    public abstract Rex getOperand();

    /**
     * @return key rex (child 1)
     */
    @NotNull
    public abstract Rex getKey();

    @Override
    @NotNull
    protected final RexType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @Override
    protected List<Operator> children() {
        Rex c0 = getOperand();
        Rex c1 = getKey();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitPathKey(this, ctx);
    }

    private static class Impl extends RexPathKey {

        private final Rex operand;
        private final Rex key;

        private Impl(@NotNull Rex operand, @NotNull Rex key) {
            this.operand = operand;
            this.key = key;
        }

        @NotNull
        @Override
        public Rex getOperand() {
            return operand;
        }

        @NotNull
        @Override
        public Rex getKey() {
            return key;
        }
    }
}
