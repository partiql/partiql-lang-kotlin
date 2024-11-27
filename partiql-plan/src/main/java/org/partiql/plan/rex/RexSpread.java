package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.types.PType;

import java.util.List;

/**
 * Logical spread expression abstract base class.
 */
public abstract class RexSpread extends RexBase {

    /**
     * @return list of spread arguments (the children)
     */
    public abstract List<Rex> getArgs();

    @NotNull
    @Override
    protected final RexType type() {
        return new RexType(PType.struct());
    }

    @NotNull
    @Override
    protected final List<Operator> children() {
        List<Rex> varargs = getArgs().stream().toList();
        return List.copyOf(varargs);
    }

    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitSpread(this, ctx);
    }
}
