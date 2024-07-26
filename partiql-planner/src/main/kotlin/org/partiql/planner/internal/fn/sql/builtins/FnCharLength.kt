// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.ClobValue
import org.partiql.value.Int32Value
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.CLOB
import org.partiql.value.PartiQLValueType.INT32
import org.partiql.value.PartiQLValueType.STRING
import org.partiql.value.PartiQLValueType.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.check
import org.partiql.value.int32Value

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_CHAR_LENGTH__STRING__INT : Fn {

    override val signature = FnSignature(
        name = "char_length",
        returns = INT32,
        parameters = listOf(
            FnParameter("value", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int32Value {
        val value = args[0].check<StringValue>().value!!
        return int32Value(value.codePointCount(0, value.length))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_CHAR_LENGTH__SYMBOL__INT : Fn {

    override val signature = FnSignature(
        name = "char_length",
        returns = INT32,
        parameters = listOf(
            FnParameter("lhs", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int32Value {
        val value = args[0].check<SymbolValue>().value!!
        return int32Value(value.codePointCount(0, value.length))
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_CHAR_LENGTH__CLOB__INT : Fn {

    override val signature = FnSignature(
        name = "char_length",
        returns = INT32,
        parameters = listOf(
            FnParameter("lhs", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int32Value {
        val value = args[0].check<ClobValue>().value!!
        return int32Value(value.size)
    }
}
