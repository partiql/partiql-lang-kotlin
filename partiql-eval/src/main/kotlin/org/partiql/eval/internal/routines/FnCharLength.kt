// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.ClobValue
import org.partiql.value.Int32Value
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.CLOB
import org.partiql.value.PType.Kind.INT
import org.partiql.value.PType.Kind.STRING
import org.partiql.value.PType.Kind.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.SymbolValue
import org.partiql.value.check
import org.partiql.value.int32Value


internal object Fn_CHAR_LENGTH__STRING__INT : Routine {

    override val signature = FnSignature(
        name = "char_length",
        returns = INT,
        parameters = listOf(
            FnParameter("value", STRING),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Int32Value {
        val value = args[0].check<StringValue>().value!!
        return int32Value(value.codePointCount(0, value.length))
    }
}


internal object Fn_CHAR_LENGTH__SYMBOL__INT : Routine {

    override val signature = FnSignature(
        name = "char_length",
        returns = INT,
        parameters = listOf(
            FnParameter("lhs", SYMBOL),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Int32Value {
        val value = args[0].check<SymbolValue>().value!!
        return int32Value(value.codePointCount(0, value.length))
    }
}


internal object Fn_CHAR_LENGTH__CLOB__INT : Routine {

    override val signature = FnSignature(
        name = "char_length",
        returns = INT,
        parameters = listOf(
            FnParameter("lhs", CLOB),
        ),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Int32Value {
        val value = args[0].check<ClobValue>().value!!
        return int32Value(value.size)
    }
}
