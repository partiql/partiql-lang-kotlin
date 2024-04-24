package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValueExperimental

internal class RelFilter(
    val input: Operator.Relation,
    val expr: Operator.Expr
) : RelPeeking() {

    private lateinit var env: Environment

    override fun openPeeking(env: Environment) {
        this.env = env
        input.open(env)
    }

    override fun peek(): Record? {
        for (inputRecord in input) {
            if (conditionIsTrue(inputRecord, expr)) {
                return inputRecord
            }
        }
        return null
    }

    override fun closePeeking() {
        input.close()
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun conditionIsTrue(record: Record, expr: Operator.Expr): Boolean {
        val condition = expr.eval(env.push(record))
        return condition is BoolValue && condition.value == true
    }
}
