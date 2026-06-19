package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal val Fn_MAP_GET__MAP_ANY__ANY: FnOverload = object : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("map_get", listOf(PType.map(PType.string(), PType.dynamic()), PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val mapType = args[0]
        if (mapType.code() != PType.MAP) return null
        val keyType = mapType.keyType
        return Function.instance(
            name = "map_get",
            returns = mapType.valueType,
            parameters = arrayOf(
                Parameter("map", mapType),
                Parameter("key", keyType),
            ),
        ) { fnArgs ->
            val map = fnArgs[0]
            val key = fnArgs[1]
            map.get(key).orElseThrow { PErrors.mapKeyNotFoundException(key, map.type) }
        }
    }
}