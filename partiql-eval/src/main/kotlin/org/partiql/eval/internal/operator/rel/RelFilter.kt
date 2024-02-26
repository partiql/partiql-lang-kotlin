package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValueExperimental

internal class RelFilter(
    val input: Operator.Relation,
    val expr: Operator.Expr
) : RelMaterialized() {

    override fun open() {
        input.open()
    }

    override fun materializeNext(): Record? {
        while (input.hasNext()) {
            val inputRecord: Record = input.next()
            if (conditionIsTrue(inputRecord, expr)) {
                return inputRecord
            }
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
