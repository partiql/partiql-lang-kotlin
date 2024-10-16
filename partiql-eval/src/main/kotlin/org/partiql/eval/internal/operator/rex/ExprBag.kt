package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum

/**
 * Creates a bag by evaluating each input value expression.
 */
internal class ExprBag(values: List<Operator.Expr>) : Operator.Expr {

    // DO NOT USE FINAL
    private var _values = values

    override fun eval(): Datum = Datum.bag(_values.map { it.eval() })
}
