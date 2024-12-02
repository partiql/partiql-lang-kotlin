package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.Operator;

/**
 * A Rel is an {@link Operator} that produces a collection of tuples.
 */
public interface Rel extends Operator {

    /**
     * @return RelType.
     */
    @NotNull
    RelType getType();
}
