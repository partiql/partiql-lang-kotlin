package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValueExperimental

internal class RelFilter(
    val input: Operator.Relation,
    val expr: Operator.Expr
) : Operator.Relation {

    override fun open() {
        input.open()
    }

    override fun next(): Record? {
        var inputRecord: Record? = input.next()
        while (inputRecord != null) {
            if (conditionIsTrue(inputRecord, expr)) {
                return inputRecord
            }
            inputRecord = input.next()
        }
        return null
    }

    override fun close() {
        input.close()
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun conditionIsTrue(record: Record, expr: Operator.Expr): Boolean {
        val condition = expr.eval(record)
        return condition is BoolValue && condition.value == true
    }
}
