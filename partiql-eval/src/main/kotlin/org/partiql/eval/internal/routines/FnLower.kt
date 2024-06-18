// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Datum
import org.partiql.value.PType.Kind.CLOB
import org.partiql.value.PType.Kind.STRING
import org.partiql.value.PType.Kind.SYMBOL
import org.partiql.value.StringValue
import org.partiql.value.check
import org.partiql.value.stringValue


internal object Fn_LOWER__STRING__STRING : Routine {

    override val signature = FnSignature(
        name = "lower",
        returns = STRING,
        parameters = listOf(FnParameter("value", STRING)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].check<StringValue>().string!!
        val result = string.lowercase()
        return stringValue(result)
    }
}


internal object Fn_LOWER__SYMBOL__SYMBOL : Routine {

    override val signature = FnSignature(
        name = "lower",
        returns = SYMBOL,
        parameters = listOf(FnParameter("value", SYMBOL)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].check<StringValue>().string!!
        val result = string.lowercase()
        return stringValue(result)
    }
}


internal object Fn_LOWER__CLOB__CLOB : Routine {

    override val signature = FnSignature(
        name = "lower",
        returns = CLOB,
        parameters = listOf(FnParameter("value", CLOB)),
        isNullCall = true,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val string = args[0].check<StringValue>().string!!
        val result = string.lowercase()
        return stringValue(result)
    }
}
