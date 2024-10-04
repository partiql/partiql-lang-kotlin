package org.partiql.plan.rel

import org.partiql.types.Field

/**
 * Analogous to a ROW type.
 *
 * TODO does not need to be an interface.
 */
public interface RelType {

    public fun getSize(): Int = getFields().size

    public fun getFields(): List<Field>

    public fun getField(name: String): Field
}
