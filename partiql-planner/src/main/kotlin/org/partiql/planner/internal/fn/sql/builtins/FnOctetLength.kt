package org.partiql.planner.internal.fn.sql.builtins

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.ClobValue
import org.partiql.value.Int32Value
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.StringValue
import org.partiql.value.check
import org.partiql.value.int32Value

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_OCTET_LENGTH__STRING__INT32 : Fn {

    override val signature = FnSignature(
        name = "octet_length",
        returns = PartiQLValueType.INT32,
        parameters = listOf(
            FnParameter("value", PartiQLValueType.STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int32Value {
        val value = args[0].check<StringValue>().value!!
        val length = value.toByteArray(Charsets.UTF_8).size
        return int32Value(length)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_OCTET_LENGTH__SYMBOL__INT32 : Fn {

    override val signature = FnSignature(
        name = "octet_length",
        returns = PartiQLValueType.INT32,
        parameters = listOf(
            FnParameter("value", PartiQLValueType.SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int32Value {
        val value = args[0].check<StringValue>().value!!
        val length = value.toByteArray(Charsets.UTF_8).size
        return int32Value(length)
    }
}

@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
internal object Fn_OCTET_LENGTH__CLOB__INT32 : Fn {

    override val signature = FnSignature(
        name = "octet_length",
        returns = PartiQLValueType.INT32,
        parameters = listOf(
            FnParameter("value", PartiQLValueType.CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): Int32Value {
        val value = args[0].check<ClobValue>().value!!
        return int32Value(value.size)
    }
}
