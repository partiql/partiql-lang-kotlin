package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;

import java.util.List;

/**
 * Abstract base class for all relational operators.
 */
public abstract class RelBase implements Rel {

    private int tag = 0;
    private RelType type;
    private List<Operator> children;

    @Override
    public int getTag() {
        return tag;
    }

    @Override
    public void setTag(int tag) {
        this.tag = tag;
    }

    @NotNull
    @Override
    public final RelType getType() {
        if (type == null) {
            type = type();
        }
        return type;
    }

    @NotNull
    @Override
    public final Operator getChild(int index) {
        return children.get(index);
    }

    @NotNull
    @Override
    public final List<Operator> getChildren() {
        if (children == null) {
            children = children();
        }
        return children;
    }

    /**
     * PROTECTED (could also be package private atm).
     *
     * @return computed type.
     */
    protected abstract RelType type();

    /**
     * PROTECTED (could also be package private atm).
     *
     * @return computed children.
     */
    protected abstract List<Operator> children();
}
