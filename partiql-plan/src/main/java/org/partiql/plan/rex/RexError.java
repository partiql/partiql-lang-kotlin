package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.OperatorVisitor;
import org.partiql.types.PType;

import java.util.List;

/**
 * This represents scenarios in which certain operations are statically known to fail in strict mode but return missing
 * in permissive mode.
 */
public abstract class RexError extends RexBase {

    /**
     * @return new RexError instance
     */
    @NotNull
    public static RexError create() {
        return new Impl();
    }

    @Override
    protected RexType type() {
        // TODO SHOULD BE UNKNOWN
        return RexType.of(PType.dynamic());
    }

    @Override
    protected List<Operator> operands() {
        return List.of();
    }

    @Override
    public <R, C> R accept(OperatorVisitor<R, C> visitor, C ctx) {
        return visitor.visitError(this, ctx);
    }

    private static class Impl extends RexError {
        // empty
    }
}
