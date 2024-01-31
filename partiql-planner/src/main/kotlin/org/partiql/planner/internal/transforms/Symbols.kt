package org.partiql.planner.internal.transforms

import org.partiql.plan.Catalog
import org.partiql.plan.builder.CatalogBuilder
import org.partiql.plan.catalogItemFn
import org.partiql.plan.catalogItemValue
import org.partiql.planner.internal.ir.Ref
import org.partiql.spi.fn.FnExperimental
import org.partiql.plan.Ref as CatalogRef

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

    fun insert(ref: Ref.Obj): CatalogRef = insert(
        catalog = ref.catalog,
        item = catalogItemValue(ref.path, ref.type),
    )

    @OptIn(FnExperimental::class)
    fun insert(ref: Ref.Fn): CatalogRef = insert(
        catalog = ref.catalog,
        item = catalogItemFn(ref.path, ref.signature.specific),
    )

    private fun insert(catalog: String, item: Catalog.Item): CatalogRef {
        val i = upsert(catalog)
        val c = catalogs[i]
        var j = 0
        while (j < c.items.size) {
            if (c.items[j] == item) {
                break
            }
            j++
        }
        c.items.add(item)
        return CatalogRef(i, j)
    }

    private fun upsert(catalog: String): Int {
        catalogs.forEachIndexed { i, c ->
            if (c.name == catalog) return i
        }
        catalogs.add(CatalogBuilder(name = catalog))
        return catalogs.size - 1
    }
}
