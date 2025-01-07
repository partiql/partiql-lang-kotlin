// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal val Fn_IS_DATE__ANY__BOOL = FunctionUtils.hidden(

    name = "is_date",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

) { args ->
    Datum.bool(args[0].type.code() == PType.DATE)
}
