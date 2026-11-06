// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.spi.function.builtins

import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils

/**
 * Returns the session identity supplied by the planner.
 *
 * SQL `CURRENT_USER` is lowered to this hidden call. The argument is
 * [org.partiql.spi.catalog.Session.getIdentity]. The evaluator has no separate user field.
 * [org.partiql.spi.catalog.Session.Builder] defaults that identity to `"unknown"` when the
 * caller does not set one, and `CURRENT_USER` returns that string.
 */
internal val Fn_CURRENT_USER__STRING__STRING = FunctionUtils.hidden(

    name = "current_user",
    returns = PType.string(),
    parameters = arrayOf(
        Parameter("identity", PType.string()),
    ),

) { args ->
    args[0]
}
