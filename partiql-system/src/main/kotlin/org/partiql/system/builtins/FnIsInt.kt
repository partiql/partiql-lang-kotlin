// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.system.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

private val INT_TYPES = setOf(
    PType.TINYINT,
    PType.SMALLINT,
    PType.INTEGER,
    PType.BIGINT,
    PType.NUMERIC
)

internal val Fn_IS_INT__ANY__BOOL = Function.static(
    name = "is_int",
    returns = PType.bool(),
    parameters = arrayOf(Parameter("value", PType.dynamic()))
) { args ->
    val arg = args[0]
    Datum.bool(arg.type.code() in INT_TYPES)
}
