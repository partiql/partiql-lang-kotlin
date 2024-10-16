package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum

internal class ExprNullIf(
    private val v1: Operator.Expr,
    private val v2: Operator.Expr,
) : Operator.Expr {

    private val comparator = Datum.comparator()

    override fun eval(): Datum {
        val d1 = v1.eval()
        val d2 = v2.eval()
        return when (comparator.compare(d1, d2)) {
            0 -> Datum.nullValue()
            else -> d1
        }
    }
}
