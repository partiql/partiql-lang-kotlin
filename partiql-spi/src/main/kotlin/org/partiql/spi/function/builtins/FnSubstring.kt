// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.utils.StringUtils.codepointSubstring
import org.partiql.spi.value.Datum

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
 * Accepts CHAR, VARCHAR, CLOB, and STRING for `value`. The declared type of the result is the
 * declared type of `value` (following the SQL <string value function> convention used by
 * [FnUpper]/[FnLower]):
 * - CHAR(n)    -> CHAR(n)
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
        val valueType = args[0]
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance("substring", PType.string(), args)
        }
        if (!valueType.isText()) return null
        return Function.instance(
            name = "substring",
            returns = valueType.asReturn(),
            parameters = arrayOf(Parameter("value", valueType), Parameter("start", PType.integer())),
        ) { params ->
            val value = params[0].text(valueType)
            val start = params[1].int
            valueType.datum(value.codepointSubstring(start))
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
        val valueType = args[0]
        // If any argument is UNKNOWN (literal NULL), return a resolution-only instance; the framework's isNullCall handles propagation.
        if (args.any { it.code() == PType.UNKNOWN }) {
            return FnUtils.nullResolutionInstance("substring", PType.string(), args)
        }
        if (!valueType.isText()) return null
        return Function.instance(
            name = "substring",
            returns = valueType.asReturn(),
            parameters = arrayOf(Parameter("value", valueType), Parameter("start", PType.integer()), Parameter("length", PType.integer())),
        ) { params ->
            val value = params[0].text(valueType)
            val start = params[1].int
            val length = params[2].int
            if (length < 0) {
                throw PErrors.internalErrorException(IllegalArgumentException("Length must be non-negative."))
            }
            valueType.datum(value.codepointSubstring(start, length))
        }
    }
}

private fun PType.isText(): Boolean = when (this.code()) {
    PType.CHAR, PType.VARCHAR, PType.CLOB, PType.STRING -> true
    else -> false
}

private fun PType.asReturn(): PType = when (this.code()) {
    PType.CHAR -> PType.character()
    PType.VARCHAR -> PType.varchar()
    PType.CLOB -> PType.clob()
    else -> PType.string()
}

/**
 * Reads a text [Datum] as a [String], handling CLOB (byte-backed) separately from the
 * character types (CHAR/VARCHAR/STRING).
 */
private fun Datum.text(type: PType): String = when (type.code()) {
    PType.CLOB -> this.bytes.toString(Charsets.UTF_8)
    else -> this.string
}

/**
 * Wraps a result [String] as a [Datum] matching the input's type family.
 */
private fun PType.datum(result: String): Datum = when (this.code()) {
    PType.CHAR -> Datum.character(result)
    PType.VARCHAR -> Datum.varchar(result)
    PType.CLOB -> Datum.clob(result.toByteArray())
    else -> Datum.string(result)
}
