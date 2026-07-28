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
import org.partiql.spi.utils.StringUtils.codepointTrim

/**
 * SQL TRIM function implementation.
 *
 * Implements the SQL <trim function> as defined in SQL2023 section 6.33 <string value function>.
 *
 * According to SQL specification:
 * - For CHAR/VARCHAR: result type is variable-length character string (VARCHAR) with maximum length equal to the input length
 * - For CLOB: result type is character large object type (CLOB) with maximum length equal to the input length
 *
 * PartiQL extensions:
 * - STRING type (PartiQL-specific unlimited length string) preserves its type
 *
 * Type preservation behavior:
 * - CHAR(n) → VARCHAR(n)
 * - VARCHAR(n) → VARCHAR(n)
 * - CLOB(n) → CLOB(n)
 * - STRING → STRING (PartiQL extension)
 */
internal object FnTrim : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(FunctionUtils.hide("trim"), listOf(PType.dynamic()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        // The argument must be a text type (or UNKNOWN, handled below); anything else does not match.
        val inputType = args[0]
        if (!inputType.isTextOrUnknown()) return null
        // An UNKNOWN argument (literal NULL) gets a resolution-only instance; the framework's isNullCall handles propagation.
        if (inputType.code() == PType.UNKNOWN) {
            return FnUtils.nullResolutionInstance(FunctionUtils.hide("trim"), inputType.stringFnReturnType(), args)
        }
        // Unlike the length-changing string functions, TRIM preserves the input length:
        // CHAR(n)/VARCHAR(n) -> VARCHAR(n), CLOB(n) -> CLOB(n), STRING -> STRING. STRING carries no
        // length, so only the bounded character types pass one to the length-aware helper.
        val length = if (inputType.code() == PType.STRING) null else inputType.length
        return Function.instance(
            name = FunctionUtils.hide("trim"),
            returns = inputType.stringFnReturnType(length),
            parameters = arrayOf(Parameter("value", inputType)),
        ) { params ->
            val value = params[0].textValue(inputType)
            inputType.stringFnResult(value.codepointTrim(), length)
        }
    }
}
