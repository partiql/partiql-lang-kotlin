// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * Function (operator) for the `IS UNKNOWN` special form.
 */
internal val Fn_IS_UNKNOWN__ANY__BOOL = Function.static(
    name = "is_unknown",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.bool())),
    isNullCall = false,
    isMissingCall = false
) { args ->
    val arg = args[0]
    if (arg.isNull || arg.isMissing) {
        Datum.bool(true)
    } else {
        Datum.bool(false)
    }
}
