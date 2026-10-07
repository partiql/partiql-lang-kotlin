package org.partiql.eval.internal.window

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.value.Datum

internal class LagFunction(
    private val expr: ExprValue,
    private val offset: ExprValue,
    private val default: ExprValue,
    private val ignoreNulls: Boolean = false,
) : NavigationFunction() {

    override fun eval(env: Environment): Datum {
        val offsetLong = offset.eval(env).long
        return navigate(env, expr, -offsetLong, default, ignoreNulls)
    }
}
