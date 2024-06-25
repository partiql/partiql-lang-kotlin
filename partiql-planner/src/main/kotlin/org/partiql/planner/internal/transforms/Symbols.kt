package org.partiql.planner.internal.transforms

import org.partiql.plan.Catalog
import org.partiql.plan.builder.CatalogBuilder
import org.partiql.plan.catalogItemAgg
import org.partiql.plan.catalogItemFn
import org.partiql.plan.catalogItemValue
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.metadata.Routine
import org.partiql.types.PType
import org.partiql.plan.Ref as CatalogRef

/**
 * Symbols is a helper class for maintaining resolved catalog symbols during planning.
 */
internal class Symbols private constructor() {

    private val catalogs: MutableList<CatalogBuilder> = mutableListOf()

    companion object {

        @JvmStatic
        fun empty() = Symbols()

        @JvmStatic
        fun create(routine: Routine): String {
            val prefix = when (routine) {
                is Routine.Aggregation -> "AGG"
                is Routine.Operator -> "OP"
                is Routine.Scalar -> "FN"
            }
            val name = routine.getName().uppercase()
            val params = routine.getParameters().joinToString("__") { it.type.specific() }
            return "${prefix}___${name}___${params}"
        }

        private fun PType.Kind.specific(): String = when (this) {
            PType.Kind.INT_ARBITRARY -> "NUMERIC"
            PType.Kind.DECIMAL_ARBITRARY -> "DECIMAL"
            PType.Kind.DOUBLE_PRECISION -> "DOUBLE"
            PType.Kind.ROW -> "STRUCT"
            else -> name.uppercase()
        }
    }

    fun build(): List<Catalog> {
        return catalogs.map { it.build() }
    }

    fun insert(ref: Ref.Obj): CatalogRef = insert(
        catalog = ref.catalog,
        item = catalogItemValue(ref.path, ref.type),
    )

    fun insert(ref: Ref.Fn): CatalogRef = insert(
        catalog = ref.catalog,
        item = catalogItemFn(ref.path, create(ref.signature)),
    )

    fun insert(ref: Ref.Agg): CatalogRef = insert(
        catalog = ref.catalog,
        item = catalogItemAgg(ref.path, create(ref.signature)),
    )

    private fun insert(catalog: String, item: Catalog.Item): CatalogRef {
        val i = upsert(catalog)
        val c = catalogs[i]
        var j = 0
        while (j < c.items.size) {
            if (c.items[j] == item) {
                // Found existing item in catalog, return the ref
                return CatalogRef(i, j)
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
