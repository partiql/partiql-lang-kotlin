package org.partiql.eval.internal

import org.partiql.plan.SymbolTable
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.ExecutionCatalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table

/**
 * Builds an array of [ExecutionCatalog] from a [Session] and [SymbolTable] for testing.
 */
internal fun buildExecutionCatalogs(symbols: SymbolTable, session: Session): Array<ExecutionCatalog> {
    return Array(symbols.catalogCount()) { catalogId ->
        val catalogName = symbols.getCatalogName(catalogId)
        val catalog = session.getCatalogs().getCatalog(catalogName)
            ?: error("Catalog '$catalogName' not found in session")
        SessionBackedExecutionCatalog(catalog, session, symbols, catalogId)
    }
}

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
}
