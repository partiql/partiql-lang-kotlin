// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

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


internal object Fn_UPPER__STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "upper",
        returns = STRING,
        parameters = listOf(FnParameter("value", STRING)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].check<StringValue>().string!!
        val result = string.uppercase()
        return stringValue(result)
    }
}


internal object Fn_UPPER__SYMBOL__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "upper",
        returns = SYMBOL,
        parameters = listOf(FnParameter("value", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].check<SymbolValue>().string!!
        val result = string.uppercase()
        return symbolValue(result)
    }
}


internal object Fn_UPPER__CLOB__CLOB : Fn {

    override val signature = FnSignature(
        name = "upper",
        returns = CLOB,
        parameters = listOf(FnParameter("value", CLOB)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val result = string.uppercase()
        return clobValue(result.toByteArray())
    }
}
