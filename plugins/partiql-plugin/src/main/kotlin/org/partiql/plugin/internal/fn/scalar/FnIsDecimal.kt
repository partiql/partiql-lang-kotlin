// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.plugin.internal.fn.scalar

import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental
import org.partiql.types.function.FunctionParameter
import org.partiql.types.function.FunctionSignature
import org.partiql.value.DecimalValue
import org.partiql.value.Int32Value
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.ANY
import org.partiql.value.PartiQLValueType.BOOL
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.boolValue
import org.partiql.value.check
import java.math.RoundingMode
import kotlin.math.max

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IS_DECIMAL__ANY__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_decimal",
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
            boolValue(arg is DecimalValue)
        }
    }
}

@OptIn(PartiQLValueExperimental::class, PartiQLFunctionExperimental::class)
internal object Fn_IS_DECIMAL__INT32_INT32_ANY__BOOL : PartiQLFunction.Scalar {

    override val signature = FunctionSignature.Scalar(
        name = "is_decimal",
        returns = BOOL,
        parameters = listOf(
            FunctionParameter("precision", INT32),
            FunctionParameter("scale", INT32),
            FunctionParameter("value", ANY)
        ),
        isNullCall = true,
        isNullable = false,
    )

    /**
     * Checks a Java BigDecimal precision and scale match PartiQL DECIMAL precision and scale.
     *
     * 1. Check that the decimal part (dp) does not exceed our scale.
     * 2. Check that the integer part (ip) length (il) does not exceed our integer part length (el = p - s).
     *
     * https://docs.oracle.com/javase/7/docs/api/java/math/BigDecimal.html
     *  > The value of the number represented by the BigDecimal is therefore (unscaledValue × 10^(-scale)).
     *  > The precision is the number of digits in the unscaled value.
     *  > If zero or positive, the scale is the number of digits to the right of the decimal point.
     *  > If negative, the unscaled value of the number is multiplied by ten to the power of the negation of the scale.
     *
     * @param args
     * @return
     */
    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val p = args[0].check<Int32Value>().value
        val s = args[1].check<Int32Value>().value
        val v = args[2]
        if (v.isNull || p == null || s == null) {
            return boolValue(null)
        }
        if (v !is DecimalValue) {
            return boolValue(false)
        }
        val d = v.value!!
        val dp = max(d.scale(), 0)
        if (dp > s) {
            return boolValue(false)
        }
        val ip = d.setScale(0, RoundingMode.DOWN)
        val il = if (ip.signum() != 0) ip.precision() - ip.scale() else 0
        val el = p - s
        return boolValue(el >= il)
    }
}
