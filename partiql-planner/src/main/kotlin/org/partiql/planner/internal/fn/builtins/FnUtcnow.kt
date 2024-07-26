// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.planner.internal.fn.builtins

import org.partiql.planner.internal.fn.Fn

import org.partiql.planner.internal.fn.FnSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType.TIMESTAMP
import org.partiql.value.datetime.TimestampWithTimeZone
import org.partiql.value.timestampValue

@OptIn(PartiQLValueExperimental::class)
internal object Fn_UTCNOW____TIMESTAMP : Fn {

    override val signature = FnSignature(
        name = "utcnow",
        returns = TIMESTAMP,
        parameters = listOf(),
        isNullCall = false,
        isNullable = false,
    )

    override fun invoke(args: Array<PartiQLValue>): PartiQLValue {
        val now = TimestampWithTimeZone.nowZ()
        return timestampValue(now)
    }
}
