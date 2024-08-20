
package org.partiql.eval.internal

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.internal.operator.rex.ExprVarGlobal
import org.partiql.plan.Catalog
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.Ref
import org.partiql.planner.catalog.Name
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.fn.Agg
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.SqlFnProvider

/**
 *
 *
 * @property catalogs
 */

internal class Symbols private constructor(private val catalogs: Array<C>) {

    private class C(
        val name: String,
        val bindings: ConnectorBindings,
        val items: Array<Catalog.Item>,
    ) {

        // TEMPORARY FOR DEPENDENCY REASONS
        fun getFn(name: Name, specific: String): Fn? = SqlFnProvider.getFn(specific)
        fun getAgg(name: Name, specific: String): Agg? = SqlFnProvider.getAgg(specific)

        override fun toString(): String = name
    }

    fun getGlobal(ref: Ref): ExprVarGlobal {
        val catalog = catalogs[ref.catalog]
        val item = catalog.items.getOrNull(ref.symbol)
        if (item == null || item !is Catalog.Item.Value) {
            error("Invalid reference $ref; missing value entry for catalog `$catalog`.")
        }
        val name = Name.of(item.path)
        return ExprVarGlobal(name, catalog.bindings)
    }

    fun getFn(ref: Ref): Fn {
        val catalog = catalogs[ref.catalog]
        val item = catalog.items.getOrNull(ref.symbol)
        if (item == null || item !is Catalog.Item.Fn) {
            error("Invalid reference $ref; missing function entry for catalog `$catalog`.")
        }
        // Lookup in connector
        val name = Name.of(item.path)
        return catalog.getFn(name, item.specific)
            ?: error("Catalog `$catalog` has no entry for function $item")
    }

    fun getAgg(ref: Ref): Agg {
        val catalog = catalogs[ref.catalog]
        val item = catalog.items.getOrNull(ref.symbol)
        if (item == null || item !is Catalog.Item.Agg) {
            error("Invalid reference $ref; missing aggregation entry for catalog `$catalog`.")
        }
        // Lookup in connector
        val name = Name.of(item.path)
        return catalog.getAgg(name, item.specific)
            ?: error("Catalog `$catalog` has no entry for aggregation function $item")
    }

    companion object {

        /**
         * Validate a plan's symbol table (plan.catalogs) and memoized necessary connector entities from the session.
         *
         * @param plan
         * @param session
         * @return
         */
        @JvmStatic
        fun build(plan: PartiQLPlan, session: PartiQLEngine.Session): Symbols {
            val catalogs = plan.catalogs.map {
                val connector = session.catalogs[it.name]
                    ?: error("The plan contains a catalog `${it.name}`, but this was absent from the engine's session")
                C(
                    name = it.name,
                    bindings = connector.getBindings(),
                    items = it.items.toTypedArray()
                )
            }.toTypedArray()
            return Symbols(catalogs)
        }
    }
}
