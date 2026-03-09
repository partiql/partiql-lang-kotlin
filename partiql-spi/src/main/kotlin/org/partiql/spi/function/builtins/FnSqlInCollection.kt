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
 * Implements SQL SOME/ANY 3-valued logic per the SQL spec:
 * - If any row comparison is True, return True.
 * - If T is empty or all comparisons are False, return False.
 * - Otherwise (at least one Unknown comparison, no True), return Null (Unknown).
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
 *   - We extract field values as an ordered list and compare element-wise against LHS
 */
private val NAME = FunctionUtils.hide("sql_in_collection")
internal val FnSqlInCollection = FnOverload.Builder(NAME)
    .addParameters(PType.dynamic(), PType.bag())
    .returns(PType.bool())
    .isNullCall(false)
    .isMissingCall(false)
    .body { args ->
        val value = args[0]
        val collection = args[1]

        // If the collection itself is null/missing, return null
        if (collection.isNull || collection.isMissing) {
            return@body Datum.nullValue(PType.bool())
        }

        val comparator = Datum.comparator()
        val lhsIsUnknown = value.isNull || value.isMissing
        var sawNull = false

        val iter = collection.iterator()
        var isEmpty = true
        while (iter.hasNext()) {
            isEmpty = false
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
                val fieldVal = fieldValues[0]
                if (fieldVal.isNull || fieldVal.isMissing || lhsIsUnknown) {
                    sawNull = true
                    continue
                }
                if (comparator.compare(value, fieldVal) == 0) {
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

            // Element-wise comparison with 3-valued logic
            var rowHasNull = false
            var allMatch = true
            for (i in lhsValues.indices) {
                val lv = lhsValues[i]
                val rv = fieldValues[i]
                if (lv.isNull || lv.isMissing || rv.isNull || rv.isMissing) {
                    rowHasNull = true
                    allMatch = false
                    // Don't break — continue to check other fields
                } else if (comparator.compare(lv, rv) != 0) {
                    allMatch = false
                    // If there's a definite mismatch (non-null != non-null),
                    // this row is False regardless of nulls in other positions
                    rowHasNull = false
                    break
                }
            }
            if (allMatch) {
                return@body Datum.bool(true)
            }
            if (rowHasNull) {
                sawNull = true
            }
        }

        // Per SQL spec: if T is empty, return False
        if (isEmpty) {
            return@body Datum.bool(false)
        }

        // No match found — if any comparison was Unknown, return null
        return@body if (sawNull) Datum.nullValue(PType.bool()) else Datum.bool(false)
    }
    .build()

private fun isCollectionType(datum: Datum): Boolean {
    return when (datum.type.code()) {
        PType.BAG, PType.ARRAY -> true
        else -> false
    }
}