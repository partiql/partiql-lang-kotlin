package org.partiql.lang.planner

import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.DEFAULT_COMPARATOR
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.ExprValueType
import java.util.TreeMap

/**
 * An extermely simple in-memory table, to be used with [InMemoryDatabase].
 */
class InMemoryTable(
    val name: String,
    primaryKeyFields: List<String>,
    private val valueFactory: ExprValueFactory
) : Iterable<ExprValue> {
    private val rows = TreeMap<ExprValue, ExprValue>(DEFAULT_COMPARATOR)

    private val primaryKeyBindingNames = primaryKeyFields.map { BindingName(it, BindingCase.SENSITIVE) }

    private fun ExprValue.extractPrimaryKey(): ExprValue =
        valueFactory.newList(
            primaryKeyBindingNames.map {
                this.bindings[it] ?: error("Row missing primary key field '${it.name}' (case: ${it.bindingCase})")
            }.asIterable()
        )

    fun containsKey(key: ExprValue): Boolean {
        require(key.type == ExprValueType.LIST) { "Primary key value must be a list" }
        return rows.containsKey(key)
    }

    val size: Int get() = rows.size

    fun truncate() {
        rows.clear()
    }

    fun insert(row: ExprValue) {
        require(row.type == ExprValueType.STRUCT) { "Row to be inserted must be a struct" }

        val primaryKeyExprValue = row.extractPrimaryKey()

        if (rows.containsKey(primaryKeyExprValue)) {
            error("Table '$name' already contains a row with the specified primary key ")
        } else {
            // We have to detatch the ExprValue from any lazily evaluated query that may get invoked
            // whenever the value is accessed.  To do this we convert to Ion, which forces full materialization,
            // and then create a new ExprValue based off the Ion.
            val rowStruct = row.ionValue
            rows[primaryKeyExprValue] = valueFactory.newFromIonValue(rowStruct)
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
