package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.List;

/**
 * Logical nullif expression abstraction base class.
 */
public abstract class RexNullIf extends RexBase {

    /**
     * @return v1 rex (child 0)
     */
    @NotNull
    public abstract Rex getV1();

    /**
     * @return v2 rex (child 1)
     */
    @NotNull
    public abstract Rex getV2();

    @NotNull
    @Override
    protected final RexType type() {
        return getV1().getType();
    }

    @Override
    protected final List<Operator> children() {
        Rex c0 = getV1();
        Rex c1 = getV2();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitNullIf(this, ctx);
    }
}
