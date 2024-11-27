package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Collation;
import org.partiql.plan.Operator;
import org.partiql.plan.Visitor;

import java.util.Collection;
import java.util.List;

/**
 * Logical sort abstract base class.
 */
public abstract class RelSort extends RelBase {

    private final RelType type = null;
    private List<Operator> children = null;

    @NotNull
    public abstract Rel getInput();

    @NotNull
    public abstract Collection<Collation> getCollations();

    @NotNull
    @Override
    public final RelType getType() {
        if (type == null) {
            throw new UnsupportedOperationException("Derive type is not implemented");
        }
        return type;
    }

    @NotNull
    @Override
    public List<Operator> getChildren() {
        if (children == null) {
            Rel c0 = getInput();
            children = List.of(c0);
        }
        return children;
    }

    @Override
    public <R, C> R accept(Visitor<R, C> visitor, C ctx) {
        return visitor.visitSort(this, ctx);
    }
}
