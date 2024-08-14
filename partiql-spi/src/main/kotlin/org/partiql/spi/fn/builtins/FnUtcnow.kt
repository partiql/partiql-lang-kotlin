// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.fn.builtins

import org.partiql.eval.value.Datum
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType
import org.partiql.value.datetime.TimestampWithTimeZone

internal object Fn_UTCNOW____TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "utcnow",
        returns = PType.timestamp(6),
        parameters = listOf(),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val now = TimestampWithTimeZone.nowZ()
        return Datum.timestampWithoutTZ(now)
    }
}
