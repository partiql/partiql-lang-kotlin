package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical path by symbol lookup expression abstract base class.
 */
public abstract class RexPathSymbol extends RexBase {

    /**
     * @return operand rex (child 0)
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

    @Override
    protected final List<Operator> children() {
        Rex c0 = getOperand();
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitPathSymbol(this, ctx);
    }
}
