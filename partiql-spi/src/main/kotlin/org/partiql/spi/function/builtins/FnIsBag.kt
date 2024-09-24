// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType
import org.partiql.types.PType.Kind

internal val Fn_IS_BAG__ANY__BOOL = Function.standard(

    name = "is_bag",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic())),

    ) { args ->
    Datum.bool(args[0].type.kind == Kind.BAG)
}
