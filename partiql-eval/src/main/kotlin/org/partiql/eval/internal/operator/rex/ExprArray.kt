package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.operator.Expression
import org.partiql.spi.value.Datum

/**
 * Creates an array by evaluating each input value expression.
 */
internal class ExprArray(values: List<Expression>) :
    Expression {

    // DO NOT USE FINAL
    private var _values = values

    override fun eval(env: Environment): Datum = Datum.list(_values.map { it.eval(env) })
}
