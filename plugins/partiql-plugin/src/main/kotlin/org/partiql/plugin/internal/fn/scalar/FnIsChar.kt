// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.errors.TypeCheckException
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.CharValue
import org.partiql.value.Int32Value
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.StringValue
import org.partiql.value.boolValue
import org.partiql.value.check

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IS_CHAR__ANY__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_char",
        returns = BOOL,
        parameters = listOf(FunctionParameter("value", ANY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val arg = args[0]
        return if (arg.isNull) {
            boolValue(null)
        } else {
            boolValue(arg is CharValue)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IS_CHAR__INT32_ANY__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_char",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("length", INT32),
            FunctionParameter("value", ANY),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val length = args[0].check<Int32Value>().value
        if (length == null || length < 0) {
            throw TypeCheckException()
        }
        val v = args[1]
        return when {
            v.isNull -> boolValue(null)
            v !is StringValue -> boolValue(false)
            else -> boolValue(v.value!!.length == length)
        }
    }
}
