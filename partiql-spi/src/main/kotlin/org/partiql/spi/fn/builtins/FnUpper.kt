// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
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
internal object Fn_UPPER__STRING__STRING : Fn {

    override val signature = FnSignature(
        name = "upper",
        returns = STRING,
        parameters = listOf(FnParameter("value", STRING)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val string = args[0].check<StringValue>().string!!
        val result = string.uppercase()
        return stringValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_UPPER__SYMBOL__SYMBOL : Fn {

    override val signature = FnSignature(
        name = "upper",
        returns = SYMBOL,
        parameters = listOf(FnParameter("value", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val string = args[0].check<SymbolValue>().string!!
        val result = string.uppercase()
        return symbolValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class)
internal object Fn_UPPER__CLOB__CLOB : Fn {

    override val signature = FnSignature(
        name = "upper",
        returns = CLOB,
        parameters = listOf(FnParameter("value", CLOB)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val string = args[0].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val result = string.uppercase()
        return clobValue(result.toByteArray())
    }
}
