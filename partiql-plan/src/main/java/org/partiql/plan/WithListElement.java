package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.partiql.plan.rex.Rex;

/**
 * <p>
 * <b>NOTE:</b> This is experimental and subject to change without prior notice!
 * </p>
 * <p>
 *
 * Experimental representation of a WITH list element in the plan. This is currently experimental
 * and is missing some core features such as the with column list.
 */
public class WithListElement {
    private final String name;
    private final Rex representation;

    public WithListElement(@NotNull String name, @NotNull Rex representation) {
        this.name = name;
        this.representation = representation;
    }

    /**
     * Returns the WITH query name.
     * @return the query name.
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Returns the query representation of this WITH list element.
     * @return query representation of this WITH list element.
     */
    @NotNull
    public Rex getRepresentation() {
        return representation;
    }

    // TODO some additional static methods once this API is stabilized
}
