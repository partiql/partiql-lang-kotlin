package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rel.Rel;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical select expression abstract base class.
 */
public abstract class RexSelect extends RexBase {

    /**
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return constructor rex (child 1)
     */
    public abstract Rex getConstructor();

    @NotNull
    @Override
    protected final RexType type() {
        return new RexType(PType.bag());
    }

    @NotNull
    @Override
    protected final List<Operator> children() {
        Rel c0 = getInput();
        Rex c1 = getConstructor();
        return List.of(c0, c1);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitSelect(this, ctx);
    }
}
