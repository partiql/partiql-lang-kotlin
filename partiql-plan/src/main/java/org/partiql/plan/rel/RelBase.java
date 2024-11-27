package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;

/**
 * Abstract base class for all relational operators.
 */
public abstract class RelBase implements Rel {

    private int tag = 0;

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
    public Operator getChild(int index) {
        return getChildren().get(index);
    }
}
