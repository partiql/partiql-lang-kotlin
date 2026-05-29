package org.partiql.eval.internal

import org.partiql.plan.SymbolTable
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.ExecutionCatalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.function.Agg
import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload

/**
 * Builds an array of [ExecutionCatalog] from a [Session] and [SymbolTable] for testing.
 * This bridges the gap between the planning-time session (which holds catalogs) and
 * the execution-time interface (which uses integer IDs).
 */
internal fun buildExecutionCatalogs(symbols: SymbolTable, session: Session): Array<ExecutionCatalog> {
    return Array(symbols.catalogCount()) { catalogId ->
        val catalogName = symbols.getCatalogName(catalogId)
        val catalog = session.getCatalogs().getCatalog(catalogName)
            ?: error("Catalog '$catalogName' not found in session")
        SessionBackedExecutionCatalog(catalog, session, symbols, catalogId)
    }
}

/**
 * An [ExecutionCatalog] backed by a [Session]'s catalog, resolving integer IDs
 * via the [SymbolTable] entries.
 */
private class SessionBackedExecutionCatalog(
    private val catalog: Catalog,
    private val session: Session,
    private val symbols: SymbolTable,
    private val catalogId: Int,
) : ExecutionCatalog {

    override fun getTable(id: Int): Table {
        val entry = symbols.getTables(catalogId)[id]
        return catalog.getTable(session, entry.name)
            ?: error("Table '${entry.name}' not found in catalog '${symbols.getCatalogName(catalogId)}'")
    }

    override fun getFn(id: Int): Fn {
        val entry = symbols.getFunctions(catalogId)[id]
        val name = entry.name.getName()
        val overloads = catalog.getFunctions(session, name)
        val paramTypes = entry.signature.parameters.map { it.type }.toTypedArray()
        for (overload in overloads) {
            val fn = overload.getInstance(paramTypes)
            if (fn != null) return fn
        }
        error("Function '${entry.name}' with matching signature not found in catalog")
    }

    override fun getFnOverload(id: Int): FnOverload {
        val entry = symbols.getFunctions(catalogId)[id]
        val name = entry.name.getName()
        val overloads = catalog.getFunctions(session, name)
        val paramTypes = entry.signature.parameters.map { it.type }
        for (overload in overloads) {
            val sig = overload.signature
            if (sig.parameterTypes == paramTypes) return overload
        }
        // Fall back to first overload with matching arity
        for (overload in overloads) {
            if (overload.signature.arity == paramTypes.size) return overload
        }
        error("FnOverload '${entry.name}' with matching signature not found in catalog")
    }

    override fun getAgg(id: Int): Agg {
        val entry = symbols.getAggregations(catalogId)[id]
        val name = entry.name.getName()
        val overloads = catalog.getAggregations(session, name)
        val paramTypes = entry.signature.parameters.map { it.type }.toTypedArray()
        for (overload in overloads) {
            val agg = overload.getInstance(paramTypes)
            if (agg != null) return agg
        }
        error("Aggregate '${entry.name}' with matching signature not found in catalog")
    }
}
