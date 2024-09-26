// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

private val INT_TYPES = setOf(
    PType.Kind.TINYINT,
    PType.Kind.SMALLINT,
    PType.Kind.INTEGER,
    PType.Kind.BIGINT,
    PType.Kind.NUMERIC
)

internal val Fn_IS_INT__ANY__BOOL = Function.static(
    name = "is_int",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic()))
) { args ->
    val arg = args[0]
    Datum.bool(arg.type.kind in INT_TYPES)
}
