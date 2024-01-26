package org.partiql.planner.internal

import org.partiql.planner.internal.ir.Catalog
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.builder.CatalogBuilder
import org.partiql.planner.internal.ir.ref

/**
 * Symbols is a helper class for maintaining resolved catalog symbols during planning.
 */
internal class Symbols private constructor() {

    private val catalogs: MutableList<CatalogBuilder> = mutableListOf()

    companion object {

        @JvmStatic
        fun empty() = Symbols()
    }

    fun build(): List<Catalog> {
        return catalogs.map { it.build() }
    }

    fun insert(catalog: String, item: Catalog.Item): Ref {
        val i = upsert(catalog)
        val c = catalogs[i]
        val j = c.items.size
        c.items.add(item)
        return ref(i, j)
    }

    private fun upsert(catalog: String): Int {
        catalogs.forEachIndexed { i, c ->
            if (c.name == catalog) return i
        }
        catalogs.add(CatalogBuilder(name = catalog))
        return catalogs.size - 1
    }
}
