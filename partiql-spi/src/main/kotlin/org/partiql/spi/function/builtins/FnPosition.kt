// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.FnUtils.textValue
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.StringUtils.codepointPosition
import org.partiql.spi.value.Datum

/**
 * SQL `POSITION(probe IN value)` function.
 *
 * Returns the 1-based position of [probe] within [value], or 0 if not found. The result is always
 * BIGINT, so — unlike the string value functions ([FnUpper]/[FnLower]) — the input type is not
 * preserved in the result.
 *
 * Accepts CHAR, VARCHAR, CLOB, and STRING for both arguments. A single dynamic overload is
 * registered and the concrete instance is resolved in [getInstance]; this avoids an ambiguous
 * match between the STRING and CLOB overloads for CHAR/VARCHAR inputs.
 */
internal object FnPosition : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("position"), listOf(PType.dynamic(), PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val probeType = args[0]
        val valueType = args[1]
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance(FunctionUtils.hide("position"), PType.bigint(), args)
        }
        if (probeType !in SqlTypeFamily.TEXT || valueType !in SqlTypeFamily.TEXT) return null
        return Function.instance(
            name = FunctionUtils.hide("position"),
            returns = PType.bigint(),
            parameters = arrayOf(Parameter("probe", probeType), Parameter("value", valueType)),
        ) { params ->
            val probe = params[0].textValue(probeType)
            val value = params[1].textValue(valueType)
            Datum.bigint(value.codepointPosition(probe).toLong())
        }
    }
}
