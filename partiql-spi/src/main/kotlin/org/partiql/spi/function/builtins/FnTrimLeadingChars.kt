// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.FnUtils.isTextOrUnknown
import org.partiql.spi.function.builtins.FnUtils.stringFnResult
import org.partiql.spi.function.builtins.FnUtils.stringFnReturnType
import org.partiql.spi.function.builtins.FnUtils.textValue
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.StringUtils.codepointTrimLeading

/**
 * `trim_leading_chars(value, chars)` — removes leading characters contained in [chars].
 *
 * Accepts CHAR, VARCHAR, CLOB, and STRING for both `value` and `chars`; each parameter keeps its own
 * argument type, so no coercion between text types is required. The result type is derived from
 * `value` alone and follows the [FnTrim] convention (length preserved, since trimming only removes
 * characters):
 * - CHAR(n)/VARCHAR(n) -> VARCHAR(n)
 * - CLOB(n)            -> CLOB(n)
 * - STRING             -> STRING
 */
internal object FnTrimLeadingChars : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("trim_leading_chars"), listOf(PType.dynamic(), PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        // Every argument must be a text type (or UNKNOWN, handled below); anything else does not match.
        if (args.any { !it.isTextOrUnknown() }) return null
        val valueType = args[0]
        val charsType = args[1]
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance(FunctionUtils.hide("trim_leading_chars"), valueType.stringFnReturnType(), args)
        }
        // Trimming only removes characters, so the result max length is the input length:
        // CHAR(n)/VARCHAR(n) -> VARCHAR(n), CLOB(n) -> CLOB(n), STRING -> STRING. STRING carries no
        // length, so only the bounded character types pass one to the length-aware helper.
        val length = if (valueType.code() == PType.STRING) null else valueType.length
        return Function.instance(
            name = FunctionUtils.hide("trim_leading_chars"),
            returns = valueType.stringFnReturnType(length),
            parameters = arrayOf(Parameter("value", valueType), Parameter("chars", charsType)),
        ) { params ->
            val value = params[0].textValue(valueType)
            val chars = params[1].textValue(charsType)
            valueType.stringFnResult(value.codepointTrimLeading(chars), length)
        }
    }
}
