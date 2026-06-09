package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_MAP_VALUES__MAP__BAG = Function.overload(

    name = "map_values",
    returns = PType.bag(),
    parameters = arrayOf(
        Parameter("map", PType.dynamic()),
    ),

) { args ->
    val map = args[0]
    val values = mutableListOf<Datum>()
    val iter = map.entries
    while (iter.hasNext()) {
        values.add(iter.next().value)
    }
    Datum.bag(values)
}
