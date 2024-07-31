package org.partiql.plan.v1

import org.partiql.types.Field

/**
 * Analogous to a ROW type.
 */
public interface Schema {

    public fun getFields(): List<Field>

    public fun getField(name: String): Field
}
