package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.internal.CoercionFamily
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal object FnMapContainsKey : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("map_contains_key", listOf(PType.map(PType.string(), PType.dynamic()), PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val mapType = args[0]
        // Accept MAP, DYNAMIC, and UNKNOWN (NULL literal) so function resolution succeeds
        // and isNullCall can handle null propagation at runtime.
        if (mapType.code() != PType.MAP && mapType.code() != PType.DYNAMIC && mapType.code() != PType.UNKNOWN) return null
        // For MAP, extract the declared key type; for DYNAMIC/UNKNOWN, accept any key type
        val mapKeyType = if (mapType.code() == PType.MAP) mapType.keyType else PType.dynamic()
        val argKeyType = args[1]
        val compatible = CoercionFamily.canCoerce(argKeyType.code(), mapKeyType.code())
        return Function.instance(
            name = "map_contains_key",
            returns = PType.bool(),
            parameters = arrayOf(
                Parameter("map", mapType),
                Parameter("key", if (compatible) mapKeyType else argKeyType),
            ),
        ) { fnArgs ->
            val map = fnArgs[0]
            val key = fnArgs[1]
            if (!compatible) {
                Datum.bool(false)
            } else {
                Datum.bool(map.containsKey(key))
            }
        }
    }
}
