package org.partiql.lang.planner.e2e

import org.partiql.lang.ION
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import java.util.UUID

class TableMetadata(val tableId: UUID, val name: String, val primaryKeyFields: List<String>)

/**
 * This is an extremely simple in-memory "database" for the purposes of demonstrating PartiQL's DML functionality
 * in the simplest manner possible.
 *
 * This database supports basic SFW and DML operations.
 *
 * A note on the separation of concerns: in a production system you'd probably want separate interfaces providing
 * to access to database schema (metadata) and its actual data.  This is intended to just be a simple demo, so we don't do
 * that here.  DL TODO?
 */
class InMemoryDatabase {
    val valueFactory = ExprValueFactory.standard(ION)
    private val tablesByName = HashMap<String, InMemoryTable>()
    private val tablesById = HashMap<UUID, InMemoryTable>()

    /**
     * Locates a table's schema by name, with optional case-insensitivity.
     *
     * Returns `null` if the table doesn't exist.
     */
    fun findTableMetadata(bindingName: BindingName): TableMetadata? {
        return when (bindingName.bindingCase) {
            // Utilize hash map if the lookup should be case-sensitive
            BindingCase.SENSITIVE -> tablesByName[bindingName.name]?.metadata
            // Search each entry one by one if the lookup should be case-insensitive
            BindingCase.INSENSITIVE ->
                tablesByName.entries
                    .firstOrNull { it.key.compareTo(bindingName.name, ignoreCase = true) == 0 }?.value?.metadata
        }
    }

    /**
     * Returns a table's metadata, given it's UUID. If no table with the given UUID exists, an an exception is
     * thrown.
     */
    fun getTableMetadata(tableId: UUID): TableMetadata =
        tablesById[tableId]?.metadata ?: error("Requested table id does not exist: $tableId")

    /**
     * Creates a table with the specified name and primary key fields.
     *
     * Currently, we assume that primary key fields are case-sensitive, but this is probably
     * incorrect. TODO: verify this and change it if needed.
     */
    fun createTable(tableName: String, primaryKeyFields: List<String>): TableMetadata {
        findTableMetadata(BindingName(tableName, BindingCase.SENSITIVE))?.let {
            error("Table with the name '$tableName' already exists!")
        }

        val metadata = TableMetadata(UUID.randomUUID(), tableName, primaryKeyFields)
        val newTable = InMemoryTable(metadata, valueFactory)
        tablesById[metadata.tableId] = newTable
        tablesByName[metadata.name] = newTable

        return metadata
    }

    private fun getTable(tableId: UUID) =
        tablesById[tableId]
            // if this happens either the table has been dropped and the plan being executed is no longer valid
            // or there's a bug in the query planner and/or one of the custom passes.
            ?: error("Table with id '$tableId' does not exist!")

    fun tableRowCount(tableId: UUID) =
        getTable(tableId).size

    fun tableContainsKey(tableId: UUID, key: ExprValue) =
        getTable(tableId).containsKey(key)

    /** Inserts the specified row.*/
    fun insert(tableId: UUID, row: ExprValue) {
        val targetTable = getTable(tableId)
        targetTable.insert(row)
    }

    /** Deletes the specified row. */
    fun delete(tableId: UUID, row: ExprValue) {
        val targetTable = getTable(tableId)
        targetTable.delete(row)
    }

    /** Gets a [Sequence] for the specified table. */
    fun getFullScanIteratable(tableId: UUID): Sequence<ExprValue> = getTable(tableId)

    fun getRecordByKey(tableId: UUID, key: ExprValue): ExprValue? {
        val targetTable = getTable(tableId)
        return targetTable[key]
    }
}
