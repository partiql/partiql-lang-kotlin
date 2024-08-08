// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType
import java.math.RoundingMode
import kotlin.math.max

internal object Fn_IS_DECIMAL__ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_decimal",
        returns = PType.typeBool(),
        parameters = listOf(FnParameter("value", PType.typeDynamic())),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        return Datum.bool(args[0].type.kind == PType.Kind.DECIMAL)
    }
}

internal object Fn_IS_DECIMAL__INT32_INT32_ANY__BOOL : Fn {

    override val signature = FnSignature(
        name = "is_decimal",
        returns = PType.typeBool(),
        parameters = listOf(
            FnParameter("type_parameter_1", PType.typeInt()),
            FnParameter("type_parameter_2", PType.typeInt()),
            FnParameter("value", PType.typeDynamic()),
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
        if (v.type.kind != PType.Kind.DECIMAL && v.type.kind != PType.Kind.DECIMAL_ARBITRARY) {
            return Datum.bool(false)
        }

        val p = args[0].int
        val s = args[1].int
        val d = v.bigDecimal
        val dp = max(d.scale(), 0)
        if (dp > s) {
            return Datum.bool(false)
        }
        val ip = d.setScale(0, RoundingMode.DOWN)
        val il = if (ip.signum() != 0) ip.precision() - ip.scale() else 0
        val el = p - s
        return Datum.bool(el >= il)
    }
}
