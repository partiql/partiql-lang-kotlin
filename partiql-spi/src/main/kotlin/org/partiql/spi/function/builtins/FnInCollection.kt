// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.FnOverload
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum

private val NAME = FunctionUtils.hide("in_collection")
internal val FnInCollection = FnOverload.Builder(NAME)
    .addParameters(PType.dynamic(), PType.bag())
    .returns(PType.bool())
    .body { args ->
        val value = args[0]
        val collection = args[1]
        val iter = collection.iterator()
        while (iter.hasNext()) {
            val v = iter.next()
            if (Datum.comparator().compare(value, v) == 0) {
                return@body Datum.bool(true)
            }
        }
        return@body Datum.bool(false)
    }
    .build()
