package org.partiql.spi.catalog.impl

import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table

/**
 * Implement a [Catalog] over an in-memory tables map.
 */
internal class StandardCatalog(
    private val name: String,
    private val tables: Map<Name, Table>,
) : Catalog {

    override fun getName(): String = name

    override fun getTable(session: Session, name: Name): Table? {
        if (name.hasNamespace()) {
            error("This catalog does not support namespaces")
        }
        return tables[name]
    }

    /**
     * TODO implement "longest match" on identifier searching.
     */
    override fun resolveTable(session: Session, identifier: Identifier): Name? {
        // TODO memory connector does not handle qualified identifiers and longest match
        val first = identifier.first()
        for ((name, _) in tables) {
            val str = name.getName() // only use single identifiers for now
            if (first.matches(str)) {
                // TODO emit errors on ambiguous table names
                return name
            }
        }
        return null
    }
}
