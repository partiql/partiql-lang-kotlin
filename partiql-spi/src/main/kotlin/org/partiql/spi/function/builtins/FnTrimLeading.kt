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
 * `trim_leading(value)` — removes leading whitespace.
 *
 * Accepts CHAR, VARCHAR, CLOB, and STRING. Follows the [FnTrim] type convention (length preserved,
 * since trimming only removes characters):
 * - CHAR(n)/VARCHAR(n) -> VARCHAR(n)
 * - CLOB(n)            -> CLOB(n)
 * - STRING             -> STRING
 */
internal object FnTrimLeading : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("trim_leading"), listOf(PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        // The argument must be a text type (or UNKNOWN, handled below); anything else does not match.
        val valueType = args[0]
        if (!valueType.isTextOrUnknown()) return null
        // An UNKNOWN argument (literal NULL) gets a resolution-only instance; the framework's isNullCall handles propagation.
        if (valueType.code() == PType.UNKNOWN) {
            return FnUtils.nullResolutionInstance(FunctionUtils.hide("trim_leading"), valueType.stringFnReturnType(), args)
        }
        // Trimming only removes characters, so the result max length is the input length:
        // CHAR(n)/VARCHAR(n) -> VARCHAR(n), CLOB(n) -> CLOB(n), STRING -> STRING. STRING carries no
        // length, so only the bounded character types pass one to the length-aware helper.
        val length = if (valueType.code() == PType.STRING) null else valueType.length
        return Function.instance(
            name = FunctionUtils.hide("trim_leading"),
            returns = valueType.stringFnReturnType(length),
            parameters = arrayOf(Parameter("value", valueType)),
        ) { params ->
            val value = params[0].textValue(valueType)
            valueType.stringFnResult(value.codepointTrimLeading(), length)
        }
    }
}
