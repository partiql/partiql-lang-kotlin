package org.partiql.eval.internal.window

import org.partiql.eval.ExprValue

internal class LagFunction(
    expr: ExprValue,
    offset: ExprValue,
    default: ExprValue,
    ignoreNulls: Boolean = false,
) : NavigationFunction(expr, offset, default, ignoreNulls, direction = -1L)
