package org.partiql.plan.rel;

import org.jetbrains.annotations.NotNull;
import org.partiql.types.Field;

/**
 * Analogous to a ROW type, consider rank/cardinality or other hint mechanisms.
 */
public interface RelType {

    public int getFieldSize();

    @NotNull
    public Field[] getFields();

    @NotNull
    public Field getField(String name);

    /**
     * @return true if the rel produces an ordered stream of rows.
     */
    public boolean isOrdered();
}
