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
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.utils.StringUtils.codepointSubstring

/**
 * Built-in function to the substring of an existing string. This function
 * propagates null and missing values as described in docs/Functions.md
 *
 * From the SQL-92 spec, page 135:
 * ```
 * 1) If <character substring function> is specified, then:
 *      a) Let C be the value of the <character value expression>,
 *      let LC be the length of C, and
 *      let S be the value of the <start position>.
 *
 *      b) If <string length> is specified, then:
 *      let L be the value of <string length> and
 *      let E be S+L.
 *      Otherwise:
 *          let E be the larger of LC + 1 and S.
 *
 *      c) If either C, S, or L is the null value, then the result of
 *      the <character substring function> is the null value.
 *
 *      d) If E is less than S, then an exception condition is raised:
 *      data exception-substring error.
 *
 *      e) Case:
 *          i) If S is greater than LC or if E is less than 1, then the
 *          result of the <character substring function> is a zero-
 *          length string.
 *
 *          ii) Otherwise,
 *              1) Let S1 be the larger of S and 1. Let E1 be the smaller
 *              of E and LC+1. Let L1 be E1-S1.
 *
 *              2) The result of the <character substring function> is
 *              a character string containing the L1 characters of C
 *              starting at character number S1 in the same order that
 *              the characters appear in C.
 *
 * Pseudocode:
 *      func substring():
 *          # Section 1-a
 *          str = <string to be sliced>
 *          strLength = LENGTH(str)
 *          startPos = <start position>
 *
 *          # Section 1-b
 *          sliceLength = <length of slice, optional>
 *          if sliceLength is specified:
 *              endPos = startPos + sliceLength
 *          else:
 *              endPos = greater_of(strLength + 1, startPos)
 *
 *          # Section 1-c:
 *          if str, startPos, or (sliceLength is specified and is null):
 *              null
 *
 *          # Section 1-d
 *          if endPos < startPos:
 *              throw exception
 *
 *          # Section 1-e-i
 *          if startPos > strLength or endPos < 1:
 *              ''
 *          else:
 *              # Section 1-e-ii
 *              S1 = greater_of(startPos, 1)
 *              E1 = lesser_of(endPos, strLength + 1)
 *              L1 = E1 - S1
 *              java's substring(C, S1, E1)
 * ```
 *
 * Accepts CHAR, VARCHAR, CLOB, and STRING for `value`. The result type follows the SQL
 * <string value function> convention used by [FnTrim]: CHAR widens to VARCHAR (a substring may be
 * shorter, and a fixed-length result would have to pad), but the declared length is preserved
 * because a substring is never longer than its input:
 * - CHAR(n)    -> VARCHAR(n)
 * - VARCHAR(n) -> VARCHAR(n)
 * - CLOB(n)    -> CLOB(n)
 * - STRING     -> STRING (PartiQL extension)
 *
 * A single dynamic overload is registered per arity and the concrete instance is resolved in
 * [getInstance]; this avoids an ambiguous match between the STRING and CLOB overloads for
 * CHAR/VARCHAR inputs.
 */
internal object FnSubstringTwoArg : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("substring", listOf(PType.dynamic(), PType.integer()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        // `value` must be a text type (or UNKNOWN, handled below); the integer args are fixed by the signature.
        val valueType = args[0]
        if (!valueType.isTextOrUnknown()) return null
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance("substring", valueType.stringFnReturnType(), listOf(valueType, PType.integer()).toTypedArray())
        }
        // A substring is never longer than its input, so the result max length is the input length:
        // CHAR(n)/VARCHAR(n) -> VARCHAR(n), CLOB(n) -> CLOB(n), STRING -> STRING. STRING carries no
        // length, so only the bounded character types pass one to the length-aware helper.
        val length = if (valueType.code() == PType.STRING) null else valueType.length
        return Function.instance(
            name = "substring",
            returns = valueType.stringFnReturnType(length),
            parameters = arrayOf(Parameter("value", valueType), Parameter("start", PType.integer())),
        ) { params ->
            val value = params[0].textValue(valueType)
            val start = params[1].int
            valueType.stringFnResult(value.codepointSubstring(start), length)
        }
    }
}

/**
 * The `substring(value, start, length)` overload; see [FnSubstringTwoArg] for the full SQL-92
 * semantics. `length` is the number of characters to take (the SQL `FOR <length>` clause), not an
 * end position. Raises a data exception if `length` is negative.
 */
internal object FnSubstringThreeArg : FnOverload() {

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature("substring", listOf(PType.dynamic(), PType.integer(), PType.integer()))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        // `value` must be a text type (or UNKNOWN, handled below); the integer args are fixed by the signature.
        val valueType = args[0]
        if (!valueType.isTextOrUnknown()) return null
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance("substring", valueType.stringFnReturnType(), listOf(valueType, PType.integer(), PType.integer()).toTypedArray())
        }
        // A substring is never longer than its input, so the result max length is the input length:
        // CHAR(n)/VARCHAR(n) -> VARCHAR(n), CLOB(n) -> CLOB(n), STRING -> STRING. STRING carries no
        // length, so only the bounded character types pass one to the length-aware helper.
        val returnLength = if (valueType.code() == PType.STRING) null else valueType.length
        return Function.instance(
            name = "substring",
            returns = valueType.stringFnReturnType(returnLength),
            parameters = arrayOf(Parameter("value", valueType), Parameter("start", PType.integer()), Parameter("length", PType.integer())),
        ) { params ->
            val value = params[0].textValue(valueType)
            val start = params[1].int
            val length = params[2].int
            if (length < 0) {
                throw PErrors.internalErrorException(IllegalArgumentException("Length must be non-negative."))
            }
            valueType.stringFnResult(value.codepointSubstring(start, length), returnLength)
        }
    }
}
