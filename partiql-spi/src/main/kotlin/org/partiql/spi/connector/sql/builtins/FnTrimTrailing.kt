// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.connector.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.StaticType
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

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TRIM_TRAILING__STRING__STRING : Fn {

    override val signature = object : FnSignature(
        name = "trim_trailing",
        returns = STRING,
        parameters = listOf(FnParameter("value", STRING)),
        isNullCall = true,
        isNullable = false
    ) {
        override fun computeReturnType(vararg parameterType: StaticType): StaticType = StaticType.STRING
    }

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<StringValue>().string!!
        val result = value.codepointTrimTrailing()
        return stringValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TRIM_TRAILING__SYMBOL__SYMBOL : Fn {

    override val signature = object : FnSignature(
        name = "trim_trailing",
        returns = SYMBOL,
        parameters = listOf(FnParameter("value", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    ) {}

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val value = args[0].check<SymbolValue>().string!!
        val result = value.codepointTrimTrailing()
        return symbolValue(result)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_TRIM_TRAILING__CLOB__CLOB : Fn {

    override val signature = object : FnSignature(
        name = "trim_trailing",
        returns = CLOB,
        parameters = listOf(FnParameter("value", CLOB)),
        isNullCall = true,
        isNullable = false,
    ) {}

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val string = args[0].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val result = string.codepointTrimTrailing()
        return clobValue(result.toByteArray())
    }
}
