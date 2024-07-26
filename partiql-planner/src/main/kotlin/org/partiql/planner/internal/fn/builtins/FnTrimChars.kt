// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.builtins

import org.partiql.planner.internal.fn.Fn
import org.partiql.planner.internal.fn.FnParameter
import org.partiql.planner.internal.fn.FnSignature
import org.partiql.planner.internal.fn.utils.StringUtils.codepointTrim
import org.partiql.value.ClobValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.check
import org.partiql.value.clobValue
import org.partiql.value.stringValue
import org.partiql.value.symbolValue

@OptIn(PartiQLValueExperimental::class)
internal object Fn_TRIM_CHARS__STRING_STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "trim_chars",
        returns = STRING,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("chars", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<StringValue>().string!!
        val chars = args[1].check<StringValue>().string!!
        val result = value.codepointTrim(chars)
        return stringValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_TRIM_CHARS__SYMBOL_SYMBOL__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "trim_chars",
        returns = SYMBOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("chars", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<SymbolValue>().string!!
        val chars = args[1].check<SymbolValue>().string!!
        val result = value.codepointTrim(chars)
        return symbolValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_TRIM_CHARS__CLOB_CLOB__CLOB : Fn {

    override val signature = FnSignature(
        name = "trim_chars",
        returns = CLOB,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("chars", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val string = args[0].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val chars = args[1].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val result = string.codepointTrim(chars)
        return clobValue(result.toByteArray())
    }
}
