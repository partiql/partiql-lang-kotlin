// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
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

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IS_INT16__ANY__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_int16",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", ANY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg = args[0]
        if (arg.isNull) {
            return boolValue(null)
        }
        return when (arg) {
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
