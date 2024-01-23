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
internal object Fn_IS_INT8__ANY__BOOL : FnScalar {

    override val signature = FnSignature.Scalar(
        name = "is_int8",
        returns = BOOL,
        parameters = listOf(FnParameter("value", ANY),),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return when (val arg = args[0]) {
            is Int8Value -> boolValue(true)
            is Int16Value -> {
                val v = arg.value!!
                boolValue(Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
            }
            is Int32Value -> {
                val v = arg.value!!
                boolValue(Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
            }
            is Int64Value -> {
                val v = arg.value!!
                boolValue(Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE)
            }
            is IntValue -> {
                val v = arg.value!!
                return try {
                    v.byteValueExact()
                    boolValue(true)
                } catch (_: ArithmeticException) {
                    boolValue(false)
                }
            }
            else -> boolValue(false)
        }
    }
}
