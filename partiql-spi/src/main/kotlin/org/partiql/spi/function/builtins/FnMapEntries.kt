package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

internal val Fn_MAP_ENTRIES__MAP__BAG = Function.overload(

    name = "map_entries",
    returns = PType.array(),
    parameters = arrayOf(
        Parameter("map", PType.dynamic()),
    ),

) { args ->
    val map = args[0]
    val rows = mutableListOf<Datum>()
    val iter = map.entries
    while (iter.hasNext()) {
        val entry = iter.next()
        val row = Datum.struct(
            Field.of("key", entry.key),
            Field.of("value", entry.value)
        )
        rows.add(row)
    }
    Datum.bag(rows)
}
