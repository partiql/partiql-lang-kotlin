// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.Float32Value
import org.partiql.value.Float64Value
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.boolValue

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IS_FLOAT32__ANY__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_float32",
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
            is Float32Value -> boolValue(true)
            is Float64Value -> {
                val v = arg.value!!
                boolValue(Float.MIN_VALUE <= v && v <= Float.MAX_VALUE)
            }
            else -> boolValue(false)
        }
    }
}
