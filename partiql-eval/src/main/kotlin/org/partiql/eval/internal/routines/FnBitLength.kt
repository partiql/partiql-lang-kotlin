package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.ClobValue
import org.partiql.value.Int32Value
import org.partiql.value.Datum
import org.partiql.value.PType.Kind
import org.partiql.value.StringValue
import org.partiql.value.check
import org.partiql.value.int32Value

// SQL spec section 6.17 contains <bit length expression>

internal object Fn_BIT_LENGTH__STRING__INT : Routine {

    override val signature = FnSignature(
        name = "bit_length",
        returns = PType.Kind.INT,
        parameters = listOf(
            FnParameter("value", PType.Kind.STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Int32Value {
        val value = args[0].check<StringValue>().value!!
        val length = value.toByteArray(Charsets.UTF_8).size
        return int32Value(length * 8)
    }
}


internal object Fn_BIT_LENGTH__SYMBOL__INT : Routine {

    override val signature = FnSignature(
        name = "bit_length",
        returns = PType.Kind.INT,
        parameters = listOf(
            FnParameter("lhs", PType.Kind.SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Int32Value {
        val value = args[0].check<StringValue>().value!!
        val length = value.toByteArray(Charsets.UTF_8).size
        return int32Value(length * 8)
    }
}


internal object Fn_BIT_LENGTH__CLOB__INT : Routine {

    override val signature = FnSignature(
        name = "bit_length",
        returns = PType.Kind.INT,
        parameters = listOf(
            FnParameter("lhs", PType.Kind.CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Int32Value {
        val value = args[0].check<ClobValue>().value!!
        return int32Value(value.size * 8)
    }
}
