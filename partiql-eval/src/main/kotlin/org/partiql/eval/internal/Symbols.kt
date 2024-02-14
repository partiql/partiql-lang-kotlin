@file:OptIn(FnExperimental::class)

package org.partiql.eval.internal

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.internal.operator.rex.ExprVarGlobal
import org.partiql.plan.Catalog
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.Ref
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.spi.connector.ConnectorFnProvider
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental

/**
 *
 *
 * @property catalogs
 */
@OptIn(FnExperimental::class)
internal class Symbols private constructor(private val catalogs: Array<C>) {

    private class C(
        val name: String,
        val bindings: ConnectorBindings,
        val functions: ConnectorFnProvider,
        val items: Array<Catalog.Item>,
    ) {

        override fun toString(): String = name
    }

    fun getGlobal(ref: Ref): ExprVarGlobal {
        val catalog = catalogs[ref.catalog]
        val item = catalog.items.getOrNull(ref.symbol)
        if (item == null || item !is Catalog.Item.Value) {
            error("Invalid reference $ref; missing value entry for catalog `$catalog`.")
        }
        val path = ConnectorPath(item.path)
        return ExprVarGlobal(path, catalog.bindings)
    }

    fun getFn(ref: Ref): Fn {
        val catalog = catalogs[ref.catalog]
        val item = catalog.items.getOrNull(ref.symbol)
        if (item == null || item !is Catalog.Item.Fn) {
            error("Invalid reference $ref; missing function entry for catalog `$catalog`.")
        }
        // Lookup in connector
        val path = ConnectorPath(item.path)
        return catalog.functions.getFn(path, item.specific)
            ?: error("Catalog `$catalog` has no entry for function $item")
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
                    functions = connector.getFunctions(),
                    items = it.items.toTypedArray()
                )
            }.toTypedArray()
            return Symbols(catalogs)
        }
    }
}
