// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import java.time.OffsetDateTime
import java.time.ZoneOffset

internal val Fn_UTCNOW____TIMESTAMP = Function.static(
    name = "utcnow",
    returns = PType.timestampz(6),
    parameters = arrayOf(),
) {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    Datum.timestampz(now, 6)
}
