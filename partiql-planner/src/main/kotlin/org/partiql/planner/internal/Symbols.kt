package org.partiql.planner.internal

import org.partiql.planner.internal.ir.Catalog
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.builder.CatalogBuilder
import org.partiql.spi.connector.ConnectorFn
import org.partiql.spi.connector.ConnectorObject

/**
 * Symbols is a helper class for maintaining resolved catalog symbols during planning.
 */
internal class Symbols private constructor() {

    private val catalogs = LinkedHashMap<String, CatalogBuilder>()

    fun build(): List<Catalog> {
        return emptyList()
    }

    fun insert(item: PathItem<ConnectorObject>): Ref {
        // no-op
        TODO()
    }

    fun insert(item: PathItem<ConnectorFn>): Ref {
        // no-op
        TODO()
    }

    companion object {

        @JvmStatic
        fun empty() = Symbols()
    }
}
