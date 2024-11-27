package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rel.Rel;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical pivot expression abstract base class.
 */
public abstract class RexPivot extends RexBase {

    /**
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return key rex (child 1)
     */
    @NotNull
    public abstract Rex getKey();

    /**
     * @return value rex (child 2)
     */
    @NotNull
    public abstract Rex getValue();

    @NotNull
    @Override
    protected final RexType type() {
        return new RexType(PType.struct());
    }

    @Override
    protected final List<Operator> children() {
        Rel c0 = getInput();
        Rex c1 = getKey();
        Rex c2 = getValue();
        return List.of(c0, c1, c2);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitPivot(this, ctx);
    }
}
