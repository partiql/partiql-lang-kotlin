package org.partiql.lang.compiler.memorydb

import org.partiql.lang.ION
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.DEFAULT_COMPARATOR
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.toIonValue
import java.util.TreeMap

/**
 * An extremely simple in-memory table, to be used with [MemoryDatabase].
 */
class MemoryTable(
    val metadata: TableMetadata
) : Sequence<ExprValue> {
    private val rows = TreeMap<ExprValue, ExprValue>(DEFAULT_COMPARATOR)

    private val primaryKeyBindingNames = metadata.primaryKeyFields.map { BindingName(it, BindingCase.SENSITIVE) }

    private fun ExprValue.extractPrimaryKey(): ExprValue =
        ExprValue.newList(
            primaryKeyBindingNames.map {
                this.bindings[it] ?: error("Row missing primary key field '${it.name}' (case: ${it.bindingCase})")
            }.asIterable()
        )

    fun containsKey(key: ExprValue): Boolean {
        require(key.type == ExprValueType.LIST) { "Primary key value must be a list" }
        return rows.containsKey(key)
    }

    val size: Int get() = rows.size

    operator fun get(key: ExprValue): ExprValue? {
        require(key.type == ExprValueType.LIST) { "specified key must have type ExprValueType.LIST " }
        return rows[key]
    }

    fun insert(row: ExprValue) {
        require(row.type == ExprValueType.STRUCT) { "Row to be inserted must be a struct" }

        val primaryKeyExprValue = row.extractPrimaryKey()

        if (rows.containsKey(primaryKeyExprValue)) {
            error("Table '${this.metadata.name}' already contains a row with the specified primary key ")
        } else {
            // We have to detatch the ExprValue from any lazily evaluated query that may get invoked
            // whenever the value is accessed.  To do this we convert to Ion, which forces full materialization,
            // and then create a new ExprValue based off the Ion.
            val rowStruct = row.toIonValue(ION)
            rows[primaryKeyExprValue] = ExprValue.of(rowStruct)
        }
    }

    /**
     * Deletes a row from the table.  [row] should at least contain all the fields which make up the
     * primary key of the table, but may also contain additional rows, which are ignored.
     */
    fun delete(row: ExprValue) {
        require(row.type == ExprValueType.STRUCT) { "Row to be deleted must be a struct" }
        val primaryKey = row.extractPrimaryKey()
        rows.remove(primaryKey)
    }

    override fun iterator(): Iterator<ExprValue> =
        // the call to .toList below is important to allow the table contents to be modified during query
        // execution.  (Otherwise we will hit a ConcurrentModificationException in the case a DELETE FROM statement
        // is executed)
        rows.values.toList().iterator()
}
