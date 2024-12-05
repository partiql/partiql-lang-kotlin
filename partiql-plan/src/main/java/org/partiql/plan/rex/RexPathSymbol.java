package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operand;
import org.partiql.plan.OperatorVisitor;

import java.util.List;

/**
 * Logical path by symbol lookup expression abstract base class.
 */
public abstract class RexPathSymbol extends RexBase {

    /**
     * @return new RexPathSymbol instance
     */
    @NotNull
    public static RexPathSymbol create(@NotNull Rex operand, @NotNull String symbol) {
        return new Impl(operand, symbol);
    }

    /**
     * @return operand rex (operand 0)
     */
    @NotNull
    public abstract Rex getOperand();

    /**
     * @return symbol string
     */
    @NotNull
    public abstract String getSymbol();

    @Override
    @NotNull
    protected final RexType type() {
        throw new UnsupportedOperationException("Derive type is not implemented");
    }

    @NotNull
    @Override
    protected final List<Operand> operands() {
        Operand c0 = Operand.single(getOperand());
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitPathSymbol(this, ctx);
    }

    private static class Impl extends RexPathSymbol {

        private final Rex operand;
        private final String symbol;

        private Impl(@NotNull Rex operand, @NotNull String symbol) {
            this.operand = operand;
            this.symbol = symbol;
        }

        @NotNull
        @Override
        public Rex getOperand() {
            return operand;
        }

        @NotNull
        @Override
        public String getSymbol() {
            return symbol;
        }
    }
}
