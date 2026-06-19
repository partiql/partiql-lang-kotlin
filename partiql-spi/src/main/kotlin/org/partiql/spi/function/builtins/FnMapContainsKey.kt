package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal object FnMapContainsKey : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("map_contains_key", listOf(PType.map(PType.string(), PType.dynamic()), PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val mapType = args[0]
        if (mapType.code() != PType.MAP) return null
        val keyType = mapType.keyType
        return Function.instance(
            name = "map_contains_key",
            returns = PType.bool(),
            parameters = arrayOf(
                Parameter("map", mapType),
                Parameter("key", keyType),
            ),
        ) { fnArgs ->
            val map = fnArgs[0]
            val key = fnArgs[1]
            Datum.bool(map.containsKey(key))
        }
    }
}
