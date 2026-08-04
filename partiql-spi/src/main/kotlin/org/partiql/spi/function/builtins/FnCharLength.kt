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
 * SQL `CHAR_LENGTH` function.
 *
 * Returns the number of characters (code points) in the input string.
 *
 * Accepts CHAR, VARCHAR, CLOB, and STRING.
 */
internal object FnCharLength : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("char_length", listOf(PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        // The argument must be a text type (or UNKNOWN, handled below); anything else does not match.
        val inputType = args[0]
        if (!inputType.isTextOrUnknown()) return null
        // An UNKNOWN argument (literal NULL) gets a resolution-only instance; the framework's isNullCall handles propagation.
        if (inputType.code() == PType.UNKNOWN) {
            return FnUtils.nullResolutionInstance("char_length", PType.integer(), args)
        }
        return when (inputType.code()) {
            PType.CHAR, PType.VARCHAR, PType.STRING -> Function.instance(
                name = "char_length",
                returns = PType.integer(),
                parameters = arrayOf(Parameter("value", inputType)),
            ) { args ->
                val value = args[0].string
                Datum.integer(value.codePointCount(0, value.length))
            }
            PType.CLOB -> Function.instance(
                name = "char_length",
                returns = PType.integer(),
                parameters = arrayOf(Parameter("value", inputType)),
            ) { args ->
                Datum.integer(args[0].bytes.size)
            }
            else -> null
        }
    }
}
