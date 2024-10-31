package org.partiql.spi.catalog.impl

import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Table
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * An internal, standard table implementation backed by simple fields.
 *
 * @constructor
 * All arguments default constructor.
 *
 * @param name
 * @param schema
 * @param datum
 */
internal class StandardTable(
    private var name: Name,
    private var schema: PType,
    private var datum: Datum,
) : Table {

    override fun getName(): Name = name
    override fun getSchema(): PType = schema
    override fun getDatum(): Datum = datum

    // TODO REMOVE ME, THIS IS REQUIRED FOR EQUALITY IN UNIT TESTS
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Table) return false
        return name == other.getName()
    }

    // TODO REMOVE ME, THIS IS REQUIRED FOR EQUALITY IN UNIT TESTS
    override fun hashCode(): Int = name.hashCode()
}
