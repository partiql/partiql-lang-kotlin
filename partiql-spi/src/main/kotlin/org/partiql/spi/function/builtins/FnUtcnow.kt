// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import java.time.LocalDateTime

internal val Fn_UTCNOW____TIMESTAMP = Function.static(
    name = "utcnow",
    returns = PType.timestamp(6),
    parameters = arrayOf(),
) {
    val now = LocalDateTime.now()
    Datum.timestamp(now, 6)
}
