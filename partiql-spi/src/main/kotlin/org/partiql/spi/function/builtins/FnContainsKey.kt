package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_CONTAINS_KEY__MAP_ANY__BOOL = Function.overload(

    name = "contains_key",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("map", PType.map(PType.string(), PType.dynamic())),
        Parameter("key", PType.dynamic()),
    ),

) { args ->
    val map = args[0]
    val key = args[1]
    Datum.bool(map.containsKey(key))
}
