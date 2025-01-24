// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.utils.FunctionUtils.booleanValue
import org.partiql.spi.value.Datum

/**
 * Function (operator) for the `IS FALSE` special form.
 */
internal val Fn_IS_FALSE__ANY__BOOL = FunctionUtils.hidden(
    name = "is_false",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.bool())),
    isNullCall = false,
    isMissingCall = false
) { args ->
    val arg = args[0]
    if (arg.isNull || arg.isMissing) {
        Datum.bool(false)
    } else {
        Datum.bool(!args[0].booleanValue())
    }
}
