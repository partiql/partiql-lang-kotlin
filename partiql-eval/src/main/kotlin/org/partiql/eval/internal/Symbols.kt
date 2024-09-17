package org.partiql.eval.internal

import org.partiql.eval.PartiQLEngine
import org.partiql.plan.Catalog
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.Ref
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Session
import org.partiql.planner.catalog.Table
import org.partiql.spi.function.Aggregation
import org.partiql.spi.function.Function
import org.partiql.planner.catalog.Catalog as Cat

/**
 * TODO Symbols will be removed in the V1 plan as it is no longer necessary.
 */
internal class Symbols private constructor(private val catalogs: Array<C>) {

    private class C(
        val name: String,
        val catalog: Cat,
        val items: Array<Catalog.Item>,
    ) {

        // TEMPORARY UNTIL ENGINE USES V1 PLANS
        private val session: Session = Session.empty(catalog.getName())

        // TEMPORARY FOR DEPENDENCY REASONS
        fun getTable(name: Name): Table? = catalog.getTable(session, name)

        override fun toString(): String = name
    }

    fun getGlobal(ref: Ref): Table {
        val catalog = catalogs[ref.catalog]
        val item = catalog.items.getOrNull(ref.symbol)
        if (item == null || item !is Catalog.Item.Value) {
            error("Invalid reference $ref; missing value entry for catalog `$catalog`.")
        }
        val name = Name.of(item.path)
        return catalog.getTable(name)
            ?: error("Catalog `$catalog` has no entry for table $item")
    }

    fun getFn(ref: Ref): Function {
        val catalog = catalogs[ref.catalog]
        val item = catalog.items.getOrNull(ref.symbol)
        if (item == null || item !is Catalog.Item.Fn) {
            error("Invalid reference $ref; missing function entry for catalog `$catalog`.")
        }
        return item.function
    }

    fun getAgg(ref: Ref): Aggregation {
        val catalog = catalogs[ref.catalog]
        val item = catalog.items.getOrNull(ref.symbol)
        if (item == null || item !is Catalog.Item.Agg) {
            error("Invalid reference $ref; missing aggregation entry for catalog `$catalog`.")
        }
        return item.aggregation
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
                    catalog = connector.getCatalog(),
                    items = it.items.toTypedArray(),
                )
            }.toTypedArray()

            return Symbols(catalogs)
        }
    }
}
