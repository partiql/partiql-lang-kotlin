// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.internal.builtins.scalar

import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnScalar
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Int16Value
import org.partiql.value.Int32Value
import org.partiql.value.Int64Value
import org.partiql.value.Int8Value
import org.partiql.value.IntValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.boolValue

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_IS_INT16__ANY__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "is_int16",
        returns = BOOL,
        parameters = listOf(FnParameter("value", ANY),),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return when (val arg = args[0]) {
            is Int8Value,
            is Int16Value -> boolValue(true)
            is Int32Value -> {
                val v = arg.value!!
                boolValue(Short.MIN_VALUE <= v && v <= Short.MAX_VALUE)
            }
            is Int64Value -> {
                val v = arg.value!!
                boolValue(Short.MIN_VALUE <= v && v <= Short.MAX_VALUE)
            }
            is IntValue -> {
                val v = arg.value!!
                return try {
                    v.shortValueExact()
                    boolValue(true)
                } catch (_: ArithmeticException) {
                    boolValue(false)
                }
            }
            else -> boolValue(false)
        }
    }
}
