package org.partiql.planner.catalog

import org.partiql.types.PType

/**
 * In PartiQL, a [Table] can take on any type and is not necessarily rows+columns.
 */
public interface Table {

    /**
     * The table's name.
     */
    public fun getName(): String

    /**
     * The table's schema.
     */
    public fun getSchema(): PType = PType.typeDynamic()
}
