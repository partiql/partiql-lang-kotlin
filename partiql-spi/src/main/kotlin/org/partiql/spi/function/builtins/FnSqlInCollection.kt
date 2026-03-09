// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnOverload
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum

/**
 * SQL IN collection function for use when the RHS is a SQL SELECT subquery.
 *
 * SQL SELECT produces a collection of structs (tuples), but the IN predicate needs to compare
 * the LHS value against the *values* inside those structs, not the structs themselves.
 *
 * For single-column SELECT (e.g., `x IN (SELECT a FROM t)`):
 *   - Each element in the bag is a struct like `{'a': 1}`
 *   - We extract the single field value and compare against LHS
 *
 * For multi-column SELECT (e.g., `(1, 2) IN (SELECT a, b FROM t)`):
 *   - Each element in the bag is a struct like `{'a': 1, 'b': 2}`
 *   - We extract field values as an ordered list and compare element-wise against LHS (which should be a list/array)
 *
 * For non-struct elements, returns false.
 */
private val NAME = FunctionUtils.hide("sql_in_collection")
internal val FnSqlInCollection = FnOverload.Builder(NAME)
    .addParameters(PType.dynamic(), PType.bag())
    .returns(PType.bool())
    .body { args ->
        val value = args[0]
        val collection = args[1]
        val comparator = Datum.comparator()
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val element = iter.next()

            // Not a struct/row — skip
            if (element.type.code() != PType.STRUCT && element.type.code() != PType.ROW) {
                continue
            }

            // Collect field values from the struct
            val fieldValues = mutableListOf<Datum>()
            val fields = element.fields
            while (fields.hasNext()) {
                fieldValues.add(fields.next().value)
            }

            // Empty struct — skip
            if (fieldValues.isEmpty()) {
                continue
            }

            // Single-column SELECT: compare LHS directly against the single field value
            if (fieldValues.size == 1) {
                if (comparator.compare(value, fieldValues[0]) == 0) {
                    return@body Datum.bool(true)
                }
                continue
            }

            // Multi-column SELECT: LHS must be a collection for element-wise comparison
            if (!isCollectionType(value)) {
                continue
            }

            // Collect LHS values
            val lhsValues = mutableListOf<Datum>()
            val lhsIter = value.iterator()
            while (lhsIter.hasNext()) {
                lhsValues.add(lhsIter.next())
            }

            // Size mismatch — skip
            if (lhsValues.size != fieldValues.size) {
                continue
            }

            // Element-wise comparison
            var allMatch = true
            for (i in lhsValues.indices) {
                if (comparator.compare(lhsValues[i], fieldValues[i]) != 0) {
                    allMatch = false
                    break
                }
            }
            if (allMatch) {
                return@body Datum.bool(true)
            }
        }
        return@body Datum.bool(false)
    }
    .build()

private fun isCollectionType(datum: Datum): Boolean {
    return when (datum.type.code()) {
        PType.BAG, PType.ARRAY -> true
        else -> false
    }
}
