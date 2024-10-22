package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.value.Datum

/**
 * Creates a bag by evaluating each input value expression.
 */
internal class ExprBag(values: List<ExprValue>) :
    ExprValue {

    // DO NOT USE FINAL
    private var _values = values

    override fun eval(env: Environment): Datum = Datum.bag(_values.map { it.eval(env) })
}
