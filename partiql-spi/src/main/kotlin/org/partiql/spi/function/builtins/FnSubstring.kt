// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.PErrors
import org.partiql.spi.function.utils.StringUtils.codepointSubstring
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * Built in function to the substring of an existing string. This function
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
 */
internal val Fn_SUBSTRING__STRING_INT32__STRING = Function.static(

    name = "substring",
    returns = PType.string(),
    parameters = arrayOf(
        Parameter("value", PType.string()),
        Parameter("start", PType.integer()),
    ),

) { args ->
    val value = args[0].string
    val start = args[1].int
    val result = value.codepointSubstring(start)
    Datum.string(result)
}

internal val Fn_SUBSTRING__STRING_INT32_INT32__STRING = Function.static(

    name = "substring",
    returns = PType.string(),
    parameters = arrayOf(
        Parameter("value", PType.string()),
        Parameter("start", PType.integer()),
        Parameter("end", PType.integer()),
    ),

) { args ->
    val value = args[0].string
    val start = args[1].int
    val end = args[2].int
    if (end < 0) {
        throw PErrors.internalErrorException(IllegalArgumentException("End must be non-negative."))
    }
    val result = value.codepointSubstring(start, end)
    Datum.string(result)
}

internal val Fn_SUBSTRING__CLOB_INT64__CLOB = Function.static(

    name = "substring",
    returns = PType.clob(Int.MAX_VALUE),
    parameters = arrayOf(
        Parameter("value", PType.clob(Int.MAX_VALUE)),
        Parameter("start", PType.integer()),
    ),

) { args ->
    val value = args[0].bytes.toString(Charsets.UTF_8)
    val start = args[1].int
    val result = value.codepointSubstring(start)
    Datum.clob(result.toByteArray())
}

internal val Fn_SUBSTRING__CLOB_INT64_INT64__CLOB = Function.static(

    name = "substring",
    returns = PType.clob(Int.MAX_VALUE),
    parameters = arrayOf(
        Parameter("value", PType.clob(Int.MAX_VALUE)),
        Parameter("start", PType.integer()),
        Parameter("end", PType.integer()),
    ),

) { args ->
    val string = args[0].bytes.toString(Charsets.UTF_8)
    val start = args[1].int
    val end = args[2].int
    if (end < 0) {
        throw PErrors.internalErrorException(IllegalArgumentException("End must be non-negative."))
    }
    val result = string.codepointSubstring(start, end)
    Datum.clob(result.toByteArray())
}
