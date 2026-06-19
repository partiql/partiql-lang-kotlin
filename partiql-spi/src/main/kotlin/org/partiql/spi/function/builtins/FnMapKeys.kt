package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_MAP_KEYS__MAP__BAG = Function.overload(

    name = "map_keys",
    returns = PType.bag(),
    parameters = arrayOf(
        Parameter("map", PType.map(PType.string(), PType.dynamic())),
    ),

) { args ->
    val map = args[0]
    val keys = mutableListOf<Datum>()
    val iter = map.entries
    while (iter.hasNext()) {
        keys.add(iter.next().key)
    }
    Datum.bag(keys)
}
