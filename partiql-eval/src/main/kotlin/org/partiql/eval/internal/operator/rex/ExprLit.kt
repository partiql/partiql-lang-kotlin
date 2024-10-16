package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum

/**
 * Literal expression.
 */
internal class ExprLit(value: Datum) : Operator.Expr {

    // DO NOT USE FINAL
    private var _value = value

    override fun eval(): Datum = _value
}
