package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum

/**
 * Literal expression.
 */
internal class ExprLit(value: Datum) : Operator.Expr {

    // DO NOT USE FINAL
    private var _value = value

    override fun eval(env: Environment): Datum = _value
}
