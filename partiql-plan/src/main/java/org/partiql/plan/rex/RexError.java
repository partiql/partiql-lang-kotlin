package org.partiql.plan.rex;

import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.types.PType;

import java.util.List;

/**
 * This represents scenarios in which certain operations are statically known to fail in strict mode but return missing
 * in permissive mode.
 */
public abstract class RexError extends RexBase {

    @Override
    protected RexType type() {
        return new RexType(PType.unknown());
    }

    @Override
    protected List<Operator> children() {
        return List.of();
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitError(this, ctx);
    }
}
