package org.partiql.planner.internal.transforms

import org.partiql.plan.SymbolTable
import org.partiql.spi.catalog.Name
import org.partiql.spi.types.PType

/**
 * Builds a [SymbolTable] during plan transformation by assigning incrementing integer IDs
 * to catalogs and tables.
 *
 * Each catalog has its own independent ID space for tables.
 */
internal class SymbolTableBuilder {

    private val catalogs = mutableListOf<String>()
    private val catalogIndex = mutableMapOf<String, Int>()

    private val tables = mutableMapOf<Int, MutableList<SymbolTable.TableEntry>>()
    private val tableIndex = mutableMapOf<TableKey, Int>()

    private data class TableKey(val catalogId: Int, val name: Name)

    fun getOrAddCatalog(catalogName: String): Int {
        return catalogIndex.getOrPut(catalogName) {
            val id = catalogs.size
            catalogs.add(catalogName)
            id
        }
    }

    fun getOrAddTable(catalogName: String, name: Name, schema: PType): Pair<Int, Int> {
        val catalogId = getOrAddCatalog(catalogName)
        val key = TableKey(catalogId, name)
        val tableId = tableIndex.getOrPut(key) {
            val entries = tables.getOrPut(catalogId) { mutableListOf() }
            val id = entries.size
            entries.add(SymbolTable.TableEntry(id, name, schema))
            id
        }
        return catalogId to tableId
    }

    fun build(): SymbolTable {
        return SymbolTableImpl(
            catalogs = catalogs.toList(),
            tables = tables.mapValues { it.value.toList() },
        )
    }

    private class SymbolTableImpl(
        private val catalogs: List<String>,
        private val tables: Map<Int, List<SymbolTable.TableEntry>>,
    ) : SymbolTable {

        override fun catalogCount(): Int = catalogs.size

        override fun getCatalogName(catalogId: Int): String = catalogs[catalogId]

        override fun getTables(catalogId: Int): List<SymbolTable.TableEntry> =
            tables[catalogId] ?: emptyList()
    }
}
