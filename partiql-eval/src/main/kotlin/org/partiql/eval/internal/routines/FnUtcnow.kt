// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.eval.internal.routines

import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
import org.partiql.value.Datum
import org.partiql.value.DatumExperimental
import org.partiql.value.PartiQLValueType.TIMESTAMP
import org.partiql.value.datetime.TimestampWithTimeZone
import org.partiql.value.timestampValue


internal object Fn_UTCNOW____TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "utcnow",
        returns = TIMESTAMP,
        parameters = listOf(),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<Datum>): Datum {
        val now = TimestampWithTimeZone.nowZ()
        return timestampValue(now)
    }
}
