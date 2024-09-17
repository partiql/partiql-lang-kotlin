// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.function.FnSignature
import org.partiql.spi.function.Function
import org.partiql.types.PType
import org.partiql.value.datetime.TimestampWithTimeZone

internal object Fn_UTCNOW____TIMESTAMP : Function {

    override val signature = FnSignature(
        name = "utcnow",
        returns = PType.timestamp(6),
        parameters = listOf(),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val now = TimestampWithTimeZone.nowZ()
        return Datum.timestamp(now)
    }
}
