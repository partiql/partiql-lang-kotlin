package org.partiql.parser.internal.util

import org.partiql.value.DecimalValue
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.NumericValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import java.math.BigInteger

internal object NumberUtils {

    /**
     * We might consider a `negate` method on the NumericValue but this is fine for now and is internal.
     */
    @OptIn(PartiQLValueExperimental::class)
    internal fun NumericValue<*>.negate(): NumericValue<*> = when (this) {
        is DecimalValue -> decimalValue(value?.negate())
        is Float32Value -> float32Value(value?.let { it * -1 })
        is Float64Value -> float64Value(value?.let { it * -1 })
        is Int8Value -> when (value) {
            null -> this
            Byte.MIN_VALUE -> int16Value(value?.let { (it.toInt() * -1).toShort() })
            else -> int8Value(value?.let { (it.toInt() * -1).toByte() })
        }
        is Int16Value -> when (value) {
            null -> this
            Short.MIN_VALUE -> int32Value(value?.let { it.toInt() * -1 })
            else -> int16Value(value?.let { (it.toInt() * -1).toShort() })
        }
        is Int32Value -> when (value) {
            null -> this
            Int.MIN_VALUE -> int64Value(value?.let { it.toLong() * -1 })
            else -> int32Value(value?.let { it * -1 })
        }
        is Int64Value -> when (value) {
            null -> this
            Long.MIN_VALUE -> intValue(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE))
            else -> int64Value(value?.let { it * -1 })
        }
        is IntValue -> intValue(value?.negate())
    }
}
