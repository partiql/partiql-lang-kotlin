package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;

/**
 * A Rel is an {@link Operator} that produces a collection of tuples.
 */
public interface Rel extends Operator {

    /**
     * Returns the type of the rows produced by this rel.
     * @return type for rows produced by this rel.
     */
    @NotNull
    RelType getType();

    /**
     * Sets the type of the rows produced by this rel.
     * @param type the new type of the rows produced by this Rel.
     */
    void setType(@NotNull RelType type);
}
