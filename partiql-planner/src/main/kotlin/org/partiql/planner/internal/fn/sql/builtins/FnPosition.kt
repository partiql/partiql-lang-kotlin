// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.sql.builtins

import org.partiql.spi.connector.sql.utils.StringUtils.codepointPosition
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.ClobValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.INT64
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.check
import org.partiql.value.int64Value

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POSITION__STRING_STRING__INT64 : Fn {

    override val signature = FnSignature(
        name = "position",
        returns = INT64,
        parameters = listOf(
            FnParameter("probe", STRING),
            FnParameter("value", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val s1 = args[0].check<StringValue>().string!!
        val s2 = args[1].check<StringValue>().string!!
        val result = s2.codepointPosition(s1)
        return int64Value(result.toLong())
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POSITION__SYMBOL_SYMBOL__INT64 : Fn {

    override val signature = FnSignature(
        name = "position",
        returns = INT64,
        parameters = listOf(
            FnParameter("probe", SYMBOL),
            FnParameter("value", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val s1 = args[0].check<SymbolValue>().string!!
        val s2 = args[1].check<SymbolValue>().string!!
        val result = s2.codepointPosition(s1)
        return int64Value(result.toLong())
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_POSITION__CLOB_CLOB__INT64 : Fn {

    override val signature = FnSignature(
        name = "position",
        returns = INT64,
        parameters = listOf(
            FnParameter("probe", CLOB),
            FnParameter("value", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val s1 = args[0].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val s2 = args[1].check<ClobValue>().value!!.toString(Charsets.UTF_8)
        val result = s2.codepointPosition(s1)
        return int64Value(result.toLong())
    }
}
