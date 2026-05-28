package org.partiql.planner.internal.transforms

import org.partiql.plan.SymbolTable
import org.partiql.spi.catalog.Name
import org.partiql.spi.function.RoutineSignature
import org.partiql.spi.types.PType

/**
 * Builds a [SymbolTable] during plan transformation by assigning incrementing integer IDs
 * to catalogs, tables, functions, and aggregates.
 *
 * Each catalog has its own independent ID space for tables, functions, and aggregates.
 */
internal class SymbolTableBuilder {

    private val catalogs = mutableListOf<String>()
    private val catalogIndex = mutableMapOf<String, Int>()

    private val tables = mutableMapOf<Int, MutableList<SymbolTable.TableEntry>>()
    private val tableIndex = mutableMapOf<TableKey, Int>()

    private val functions = mutableMapOf<Int, MutableList<SymbolTable.FnEntry>>()
    private val fnIndex = mutableMapOf<FnKey, Int>()

    private val aggregations = mutableMapOf<Int, MutableList<SymbolTable.AggEntry>>()
    private val aggIndex = mutableMapOf<AggKey, Int>()

    private data class TableKey(val catalogId: Int, val name: Name)
    private data class FnKey(val catalogId: Int, val name: Name, val params: List<PType>)
    private data class AggKey(val catalogId: Int, val name: Name, val params: List<PType>)

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

    fun getOrAddFn(catalogName: String, name: Name, signature: RoutineSignature): Pair<Int, Int> {
        val catalogId = getOrAddCatalog(catalogName)
        val params = signature.parameters.map { it.type }
        val key = FnKey(catalogId, name, params)
        val fnId = fnIndex.getOrPut(key) {
            val entries = functions.getOrPut(catalogId) { mutableListOf() }
            val id = entries.size
            entries.add(SymbolTable.FnEntry(id, name, signature))
            id
        }
        return catalogId to fnId
    }

    fun getOrAddAgg(catalogName: String, name: Name, signature: RoutineSignature): Pair<Int, Int> {
        val catalogId = getOrAddCatalog(catalogName)
        val params = signature.parameters.map { it.type }
        val key = AggKey(catalogId, name, params)
        val aggId = aggIndex.getOrPut(key) {
            val entries = aggregations.getOrPut(catalogId) { mutableListOf() }
            val id = entries.size
            entries.add(SymbolTable.AggEntry(id, name, signature))
            id
        }
        return catalogId to aggId
    }

    fun build(): SymbolTable {
        return SymbolTableImpl(
            catalogs = catalogs.toList(),
            tables = tables.mapValues { it.value.toList() },
            functions = functions.mapValues { it.value.toList() },
            aggregations = aggregations.mapValues { it.value.toList() },
        )
    }

    private class SymbolTableImpl(
        private val catalogs: List<String>,
        private val tables: Map<Int, List<SymbolTable.TableEntry>>,
        private val functions: Map<Int, List<SymbolTable.FnEntry>>,
        private val aggregations: Map<Int, List<SymbolTable.AggEntry>>,
    ) : SymbolTable {

        override fun catalogCount(): Int = catalogs.size

        override fun getCatalogName(catalogId: Int): String = catalogs[catalogId]

        override fun getTables(catalogId: Int): List<SymbolTable.TableEntry> =
            tables[catalogId] ?: emptyList()

        override fun getFunctions(catalogId: Int): List<SymbolTable.FnEntry> =
            functions[catalogId] ?: emptyList()

        override fun getAggregations(catalogId: Int): List<SymbolTable.AggEntry> =
            aggregations[catalogId] ?: emptyList()
    }
}
