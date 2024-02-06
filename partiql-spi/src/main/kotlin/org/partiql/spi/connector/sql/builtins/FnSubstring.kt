// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.errors.TypeCheckException
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.ClobValue
import org.partiql.value.Int64Value
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.check
import org.partiql.value.clobValue
import org.partiql.value.stringValue
import org.partiql.value.symbolValue

/**
 * Built in function to return the substring of an existing string. This function
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
 *              return null
 *
 *          # Section 1-d
 *          if endPos < startPos:
 *              throw exception
 *
 *          # Section 1-e-i
 *          if startPos > strLength or endPos < 1:
 *              return ''
 *          else:
 *              # Section 1-e-ii
 *              S1 = greater_of(startPos, 1)
 *              E1 = lesser_of(endPos, strLength + 1)
 *              L1 = E1 - S1
 *              return java's substring(C, S1, E1)
 */
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_SUBSTRING__STRING_INT64__STRING : Fn {

    override val signature = FnSignature(
        name = "substring",
        returns = STRING,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("start", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<StringValue>().string!!
        val start = args[1].check<Int64Value>().int!!
        val result = value.codepointSubstring(start)
        return stringValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_SUBSTRING__STRING_INT64_INT64__STRING : Fn {

    override val signature = FnSignature(
        name = "substring",
        returns = STRING,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("start", INT64),
            FnParameter("end", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<StringValue>().string!!
        val start = args[1].check<Int64Value>().int!!
        val end = args[2].check<Int64Value>().int!!
        if (end < 0) throw TypeCheckException()
        val result = value.codepointSubstring(start, end)
        return stringValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_SUBSTRING__SYMBOL_INT64__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "substring",
        returns = SYMBOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("start", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<SymbolValue>().string!!
        val start = args[1].check<Int64Value>().int!!
        val result = value.codepointSubstring(start)
        return symbolValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_SUBSTRING__SYMBOL_INT64_INT64__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "substring",
        returns = SYMBOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("start", INT64),
            FnParameter("end", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<SymbolValue>().string!!
        val start = args[1].check<Int64Value>().int!!
        val end = args[2].check<Int64Value>().int!!
        if (end < 0) throw TypeCheckException()
        val result = value.codepointSubstring(start, end)
        return symbolValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_SUBSTRING__CLOB_INT64__CLOB : Fn {

    override val signature = FnSignature(
        name = "substring",
        returns = CLOB,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("start", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val start = args[1].check<Int64Value>().int!!
        val result = value.codepointSubstring(start)
        return clobValue(result.toByteArray())
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_SUBSTRING__CLOB_INT64_INT64__CLOB : Fn {

    override val signature = FnSignature(
        name = "substring",
        returns = CLOB,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("start", INT64),
            FnParameter("end", INT64),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val string = args[0].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val start = args[1].check<Int64Value>().int!!
        val end = args[2].check<Int64Value>().int!!
        if (end < 0) throw TypeCheckException()
        val result = string.codepointSubstring(start, end)
        return clobValue(result.toByteArray())
    }
}
