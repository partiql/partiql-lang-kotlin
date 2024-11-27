package org.partiql.plan.rex;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;
import org.partiql.plan.rel.Rel;
import org.partiql.types.PType;

import java.util.List;
import java.util.List;

/**
 * Logical subquery in expression abstract base class.
 */
public abstract class RexSubqueryIn extends RexBase {

    /**
     * @return input rel (child 0)
     */
    @NotNull
    public abstract Rel getInput();

    /**
     * @return collection comparison arguments (not children).
     */
    @NotNull
    public abstract List<Rex> getArgs();

    @Override
    protected final RexType type() {
        return new RexType(PType.bool());
    }

    @Override
    protected List<Operator> children() {
        Rel c0 = getInput();
        return List.of(c0);
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitSubqueryIn(this, ctx);
    }
}
