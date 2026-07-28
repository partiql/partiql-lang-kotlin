// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.FnUtils.isTextOrUnknown
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * SQL `BIT_LENGTH` function (SQL spec section 6.17 <bit length expression>).
 *
 * Returns the number of bits in the input string. The result is always an INTEGER, so — unlike the
 * string value functions ([FnUpper]/[FnLower]) — the input type is not preserved in the result.
 *
 * Accepts CHAR, VARCHAR, CLOB, and STRING. A single dynamic overload is registered and the
 * concrete instance is resolved in [getInstance]; this avoids an ambiguous match between the
 * STRING and CLOB overloads for CHAR/VARCHAR inputs.
 */
internal object FnBitLength : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("bit_length", listOf(PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        // The argument must be a text type (or UNKNOWN, handled below); anything else does not match.
        val inputType = args[0]
        if (!inputType.isTextOrUnknown()) return null
        // An UNKNOWN argument (literal NULL) gets a resolution-only instance; the framework's isNullCall handles propagation.
        if (inputType.code() == PType.UNKNOWN) {
            return FnUtils.nullResolutionInstance("bit_length", PType.integer(), args)
        }
        return when (inputType.code()) {
            PType.CHAR, PType.VARCHAR, PType.STRING -> Function.instance(
                name = "bit_length",
                returns = PType.integer(),
                parameters = arrayOf(Parameter("value", inputType)),
            ) { args ->
                val length = args[0].string.toByteArray(Charsets.UTF_8).size
                Datum.integer(length * 8)
            }
            PType.CLOB -> Function.instance(
                name = "bit_length",
                returns = PType.integer(),
                parameters = arrayOf(Parameter("value", inputType)),
            ) { args ->
                Datum.integer(args[0].bytes.size * 8)
            }
            else -> null
        }
    }
}
