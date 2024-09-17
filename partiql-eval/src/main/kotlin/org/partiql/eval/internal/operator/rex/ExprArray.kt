package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum

/**
 * Creates an array by evaluating each input value expression.
 */
internal class ExprArray(values: List<Operator.Expr>) : Operator.Expr {

    // DO NOT USE FINAL
    private var _values = values

    override fun eval(env: Environment): Datum = Datum.list(_values.map { it.eval(env) })
}
