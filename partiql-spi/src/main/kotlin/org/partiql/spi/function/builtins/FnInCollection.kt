// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnOverload
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum

/**
 * IN collection function (non-SQL style). Implements SQL SOME/ANY 3-valued logic:
 *
 * - If any element matches (True), return True.
 * - If the collection is empty, return False.
 * - If no match and all comparisons were False, return False.
 * - If no match but at least one comparison was Unknown (null/missing), return Null.
 */
private val NAME = FunctionUtils.hide("in_collection")
internal val FnInCollection = FnOverload.Builder(NAME)
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
            val v = iter.next()
            if (v.isNull || v.isMissing || lhsIsUnknown) {
                sawNull = true
                continue
            }
            if (comparator.compare(value, v) == 0) {
                return@body Datum.bool(true)
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
