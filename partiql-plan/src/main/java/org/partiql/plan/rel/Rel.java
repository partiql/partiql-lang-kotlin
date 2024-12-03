package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;

/**
 * A Rel is an {@link Operator} that produces a collection of tuples.
 */
public interface Rel extends Operator {

    /**
     * @return the type of the rows produced by this rel.
     */
    @NotNull
    public RelType getType();

    /**
     * @param type the new type of the rows produced by this Rex.
     */
    public void setType(@NotNull RelType type);
}
