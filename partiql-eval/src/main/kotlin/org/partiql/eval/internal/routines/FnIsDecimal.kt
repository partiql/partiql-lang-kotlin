// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.DecimalValue
import org.partiql.value.Int32Value
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.DYNAMIC
import org.partiql.value.PType.Kind.BOOL
import org.partiql.value.PType.Kind.INT
import org.partiql.value.boolValue
import org.partiql.value.check
import java.math.RoundingMode
import kotlin.math.max


internal object Fn_IS_DECIMAL__DYNAMIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "is_decimal",
        returns = BOOL,
        parameters = listOf(FnParameter("value", DYNAMIC)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return boolValue(args[0] is DecimalValue)
    }
}


internal object Fn_IS_DECIMAL__INT_INT_DYNAMIC__BOOL : Routine {

    override val signature = FnSignature(
        name = "is_decimal",
        returns = BOOL,
        parameters = listOf(
            FnParameter("type_parameter_1", INT),
            FnParameter("type_parameter_2", INT),
            FnParameter("value", DYNAMIC),
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
    override fun invoke(args: Array<Datum>): Datum {
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
