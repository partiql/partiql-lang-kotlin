// ktlint-disable filename
@file:Suppress("ClassName", "DEPRECATION")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_IN_COLLECTION__ANY_BAG__BOOL = Function.static(

    name = "in_collection",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
        Parameter("collection", PType.bag()),
    ),

) { args ->
    val value = args[0]
    val collection = args[1]
    val iter = collection.iterator()
    while (iter.hasNext()) {
        val v = iter.next()
        if (Datum.comparator().compare(value, v) == 0) {
            return@static Datum.bool(true)
        }
    }
    Datum.bool(false)
}

internal val Fn_IN_COLLECTION__ANY_LIST__BOOL = Function.static(

    name = "in_collection",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("value", PType.dynamic()),
        Parameter("collection", PType.array()),
    ),

) { args ->
    val value = args[0]
    val collection = args[1]
    val iter = collection.iterator()
    while (iter.hasNext()) {
        val v = iter.next()
        if (Datum.comparator().compare(value, v) == 0) {
            return@static Datum.bool(true)
        }
    }
    Datum.bool(false)
}
