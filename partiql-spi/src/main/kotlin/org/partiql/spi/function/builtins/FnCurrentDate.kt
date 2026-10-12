// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum
import java.time.LocalDate

/**
 * Direct calls return the JVM's current local date.
 *
 * SQL `CURRENT_DATE` does not invoke this body once per reference. The evaluator records one
 * [LocalDate] when a query starts and reuses it for every `CURRENT_DATE` in that execution,
 * including values read from a lazy result after execution returns.
 */
internal val Fn_CURRENT_DATE____DATE = FunctionUtils.hidden(

    name = "current_date",
    returns = PType.date(),
    parameters = arrayOf(),

) { _ ->
    Datum.date(LocalDate.now())
}
