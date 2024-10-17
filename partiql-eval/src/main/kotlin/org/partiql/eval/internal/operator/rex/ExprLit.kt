package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.operator.Expression
import org.partiql.spi.value.Datum

/**
 * Literal expression.
 */
internal class ExprLit(value: Datum) : Expression {

    // DO NOT USE FINAL
    private var _value = value

    override fun eval(env: Environment): Datum = _value
}
