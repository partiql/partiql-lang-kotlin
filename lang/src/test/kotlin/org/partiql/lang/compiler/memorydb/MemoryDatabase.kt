package org.partiql.lang.compiler.memorydb

import org.partiql.lang.ION
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import java.util.UUID

class TableMetadata(val tableId: UUID, val name: String, val primaryKeyFields: List<String>)

/**
 * This is an extremely simple in-memory "database" for the purposes of demonstrating how to use PartiQL's
 * query planner.
 *
 * This database supports basic SFW and DML operations.
 */
class MemoryDatabase {
    val valueFactory = ExprValueFactory.standard(ION)
    private val tables = ArrayList<MemoryTable>()

    /**
     * Locates a table's schema by name, with optional case-insensitivity.
     *
     * Returns `null` if the table doesn't exist.
     */
    fun findTableMetadata(bindingName: BindingName): TableMetadata? =
        tables.firstOrNull { bindingName.isEquivalentTo(it.metadata.name) }?.metadata

    /**
     * Returns a table's metadata, given its UUID. If no table with the given UUID exists, an exception is
     * thrown.
     */
    fun getTableMetadata(tableId: UUID): TableMetadata =
        tables.firstOrNull { it.metadata.tableId == tableId }?.metadata
            ?: error("Table with id '$tableId' does not exist!")

    /**
     * Creates a table with the specified name and primary key fields.
     *
     * Currently, we assume that primary key fields are case-sensitive, but this is probably
     * incorrect. DL TODO: verify this and change it if needed.
     */
    fun createTable(tableName: String, primaryKeyFields: List<String>): TableMetadata {
        findTableMetadata(BindingName(tableName, BindingCase.SENSITIVE))?.let {
            error("Table with the name '$tableName' already exists!")
        }

        val metadata = TableMetadata(UUID.randomUUID(), tableName, primaryKeyFields)
        val newTable = MemoryTable(metadata, valueFactory)
        tables.add(newTable)

        return metadata
    }

    private fun getTable(tableId: UUID) =
        tables.firstOrNull { it.metadata.tableId == tableId }
            // if this happens either the table has been dropped and the plan being executed is no longer valid
            // or there's a bug in the query planner and/or one of the custom passes.
            ?: error("Table with id '$tableId' does not exist!")

    fun getRowCount(tableId: UUID) =
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
    fun getFullScanSequence(tableId: UUID): Sequence<ExprValue> = getTable(tableId)

    fun getRecordByKey(tableId: UUID, key: ExprValue): ExprValue? {
        val targetTable = getTable(tableId)
        return targetTable[key]
    }
}
