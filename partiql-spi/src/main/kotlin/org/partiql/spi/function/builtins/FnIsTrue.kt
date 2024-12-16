// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.internal.booleanValue
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * Function (operator) for the `IS TRUE` special form.
 */
internal val Fn_IS_TRUE__ANY__BOOL = Function.static(
    name = "is_true",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.bool())),
    isNullCall = false,
    isMissingCall = false
) { args ->
    val arg = args[0]
    if (arg.isNull || arg.isMissing) {
        Datum.bool(false)
    } else {
        Datum.bool(args[0].booleanValue())
    }
}
