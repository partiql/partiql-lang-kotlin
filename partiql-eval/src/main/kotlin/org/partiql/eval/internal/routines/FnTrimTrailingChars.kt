// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.connector.sql.utils.StringUtils.codepointTrimTrailing
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.ClobValue
import org.partiql.value.Datum
import org.partiql.value.DatumExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.check
import org.partiql.value.clobValue
import org.partiql.value.stringValue
import org.partiql.value.symbolValue


internal object Fn_TRIM_TRAILING_CHARS__STRING_STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "trim_trailing_chars",
        returns = STRING,
        parameters = listOf(
            FnParameter("value", STRING),
            FnParameter("chars", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<StringValue>().string!!
        val chars = args[1].check<StringValue>().string!!
        val result = value.codepointTrimTrailing(chars)
        return stringValue(result)
    }
}


internal object Fn_TRIM_TRAILING_CHARS__SYMBOL_SYMBOL__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "trim_trailing_chars",
        returns = SYMBOL,
        parameters = listOf(
            FnParameter("value", SYMBOL),
            FnParameter("chars", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val value = args[0].check<SymbolValue>().string!!
        val chars = args[1].check<SymbolValue>().string!!
        val result = value.codepointTrimTrailing(chars)
        return symbolValue(result)
    }
}


internal object Fn_TRIM_TRAILING_CHARS__CLOB_CLOB__CLOB : Fn {

    override val signature = FnSignature(
        name = "trim_trailing_chars",
        returns = CLOB,
        parameters = listOf(
            FnParameter("value", CLOB),
            FnParameter("chars", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val chars = args[1].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val result = string.codepointTrimTrailing(chars)
        return clobValue(result.toByteArray())
    }
}
