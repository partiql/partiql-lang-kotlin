package org.partiql.plugins.memory

import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.types.PType

/**
 * Implement [Catalog] over an in-memory tables map.
 */
public class MemoryCatalog(
    private val name: String,
    private val tables: Map<Name, MemoryTable>,
) : Catalog {

    override fun getName(): String = name

    override fun getTable(session: Session, name: Name): Table? {
        if (name.hasNamespace()) {
            error("MemoryCatalog does not support namespaces")
        }
        return tables[name]
    }

    /**
     * TODO implement "longest match" on identifier searching.
     */
    override fun getTable(session: Session, identifier: Identifier): Table? {
        // TODO memory connector does not handle qualified identifiers and longest match
        val first = identifier.first()
        for ((name, table) in tables) {
            val str = name.getName() // only use single identifiers for now
            if (first.matches(str)) {
                // TODO emit errors on ambiguous table names
                return table
            }
        }
        return null
    }

    public companion object {

        @JvmStatic
        public fun builder(): Builder = Builder()
    }

    public class Builder internal constructor() {

        private var name: String? = null
        private var tables: MutableMap<Name, MemoryTable> = mutableMapOf()

        public fun name(name: String): Builder = apply { this.name = name }

        // TODO REMOVE AFTER CREATE TABLE IS ADDED TO CATALOG
        public fun define(name: String, type: PType): Builder {
            val table = MemoryTable.empty(name, type)
            return define(table)
        }

        // TODO REMOVE AFTER CREATE TABLE IS ADDED TO CATALOG
        public fun define(table: MemoryTable): Builder = apply { tables[table.getName()] = table }

        public fun build(): MemoryCatalog = MemoryCatalog(name!!, tables)
    }
}
