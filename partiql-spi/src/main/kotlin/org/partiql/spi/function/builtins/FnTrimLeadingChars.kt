// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.FnUtils.stringFnDatum
import org.partiql.spi.function.builtins.FnUtils.stringFnReturn
import org.partiql.spi.function.builtins.FnUtils.textValue
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.StringUtils.codepointTrimLeading

/**
 * `trim_leading_chars(value, chars)` — removes leading characters contained in [chars].
 *
 * Accepts CHAR, VARCHAR, CLOB, and STRING for `value`; `chars` accepts CHAR/VARCHAR/STRING
 * (implicitly coerced to STRING). Follows the [FnTrim] type convention:
 * - CHAR(n)/VARCHAR(n) -> VARCHAR
 * - CLOB(n)            -> CLOB
 * - STRING             -> STRING
 */
internal object FnTrimLeadingChars : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("trim_leading_chars"), listOf(PType.dynamic(), PType.string()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val valueType = args[0]
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance(FunctionUtils.hide("trim_leading_chars"), PType.string(), args)
        }
        if (valueType !in SqlTypeFamily.TEXT) return null
        return Function.instance(
            name = FunctionUtils.hide("trim_leading_chars"),
            returns = valueType.stringFnReturn(),
            parameters = arrayOf(Parameter("value", valueType), Parameter("chars", PType.string())),
        ) { params ->
            val value = params[0].textValue(valueType)
            val chars = params[1].string
            valueType.stringFnDatum(value.codepointTrimLeading(chars))
        }
    }
}
