package org.partiql.plan;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.partiql.plan.rex.Rex;

import java.util.List;

/**
 * <p>
 * <b>NOTE:</b> This is experimental and subject to change without prior notice!
 * </p>
 * <p>
 *
 * Experimental representation of a WITH list element in the plan. This is currently experimental.
 */
public class WithListElement {
    private final String name;
    private final Rex representation;
    private final List<String> columns;

    public WithListElement(@NotNull String name, @NotNull Rex representation) {
        this(name, representation, null);
    }

    /**
     * @param name the WITH query name.
     * @param representation the query representation, whose columns are already renamed per the column list.
     * @param columns the WITH column list, if any.
     */
    public WithListElement(@NotNull String name, @NotNull Rex representation, @Nullable List<String> columns) {
        this.name = name;
        this.representation = representation;
        this.columns = columns;
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

    /**
     * Returns the WITH column list of this WITH list element, if any. The columns of the {@link #getRepresentation()}
     * are already renamed per the column list; consumers that reproduce the WITH clause (e.g. transpilers) may use this
     * to reproduce the column list.
     * @return the column names, or null if the WITH list element has no column list.
     */
    @Nullable
    public List<String> getColumns() {
        return columns;
    }

    // TODO some additional static methods once this API is stabilized
}
