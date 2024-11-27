// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import java.math.RoundingMode
import kotlin.math.max

internal val Fn_IS_DECIMAL__ANY__BOOL = Function.static(
    name = "is_decimal",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),
) { args ->
    Datum.bool(args[0].type.code() == PType.DECIMAL)
}

internal val Fn_IS_DECIMAL__INT32_INT32_ANY__BOOL = Function.static(
    name = "is_decimal",
    returns = PType.bool(),
    parameters = arrayOf(
        Parameter("type_parameter_1", PType.integer()),
        Parameter("type_parameter_2", PType.integer()),
        Parameter("value", PType.dynamic()),
    ),
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
{ args ->
    val v = args[2]
    if (v.type.code() != PType.DECIMAL) {
        return@static Datum.bool(false)
    }
    val p = args[0].int
    val s = args[1].int
    val d = v.bigDecimal
    val dp = max(d.scale(), 0)
    if (dp > s) {
        return@static Datum.bool(false)
    }
    val ip = d.setScale(0, RoundingMode.DOWN)
    val il = if (ip.signum() != 0) ip.precision() - ip.scale() else 0
    val el = p - s
    Datum.bool(el >= il)
}
