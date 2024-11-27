package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical array expression abstract base class.
 */
public abstract class RexArray extends RexBase {

    /**
     * @return the values of the array, also the children.
     */
    @NotNull
    public abstract List<Rex> getValues();

    @NotNull
    @Override
    protected final RexType type() {
        return new RexType(PType.array());
    }

    @NotNull
    @Override
    protected final List<Operator> children() {
        List<Rex> varargs = getValues();
        return List.copyOf(varargs);
    }

    @Override
    public <R, C> R accept(@NotNull Visitor<R, C> visitor, C ctx) {
        return visitor.visitArray(this, ctx);
    }
}
