package org.partiql.planner.internal.catalog

import org.partiql.planner.internal.ir.Catalog
import org.partiql.planner.internal.ir.builder.CatalogBuilder

internal class CatalogResolver(
    private val
) {

    /**
     * Maintain all catalog items referenced during query planning.
     */
    private val refs = mutableListOf<CatalogBuilder>()

    /**
     * Builds and returns the current list of all referenced catalog items.
     *
     * @return
     */
    fun catalogs(): List<Catalog> = refs.map { it.build() }


}
