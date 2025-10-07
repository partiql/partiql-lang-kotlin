package org.partiql.eval.internal.window

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.value.Datum

internal class LagFunction(
    private val expr: ExprValue,
    private val offset: ExprValue,
    private val default: ExprValue
) : NavigationFunction() {

    override fun eval(env: Environment): Datum {
        val offsetLong = offset.eval(env).long

        // Return if out-of-bounds
        if (offsetLong > currentPosition) {
            return default.eval(env)
        }

        // Get lagged expression
        val row = partition.get(currentPosition - offsetLong)
        val newEnv = env.push(row)
        return expr.eval(newEnv)
    }

    override fun reset() {
        // Do nothing
    }
}
