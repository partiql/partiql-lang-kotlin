// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.builtins

import org.partiql.planner.internal.fn.Fn
import org.partiql.planner.internal.fn.FnParameter
import org.partiql.planner.internal.fn.FnSignature
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

@OptIn(PartiQLValueExperimental::class)
internal object Fn_IS_DECIMAL__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_decimal",
        returns = BOOL,
        parameters = listOf(FnParameter("value", ANY)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        return boolValue(args[0] is DecimalValue)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_IS_DECIMAL__INT32_INT32_ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_decimal",
        returns = BOOL,
        parameters = listOf(
            FnParameter("type_parameter_1", INT32),
            FnParameter("type_parameter_2", INT32),
            FnParameter("value", ANY),
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
        val v = args[2]
        if (v !is DecimalValue) {
            return boolValue(false)
        }

        val p = args[0].check<Int32Value>().value!!
        val s = args[1].check<Int32Value>().value!!
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
