package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType

internal val Fn_MAP_GET__MAP_ANY__ANY = Function.overload(

    name = "map_get",
    returns = PType.dynamic(),
    parameters = arrayOf(
        Parameter("map", PType.dynamic()),
        Parameter("key", PType.dynamic()),
    ),

) { args ->
    val map = args[0]
    val key = args[1]
    map.get(key).orElse(null)
}
