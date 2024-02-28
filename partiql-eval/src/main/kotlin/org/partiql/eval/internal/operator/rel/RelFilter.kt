package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValueExperimental

internal class RelFilter(
    val input: Operator.Relation,
    val expr: Operator.Expr
) : RelMaterialized() {

    private lateinit var env: Environment

    override fun open(env: Environment) {
        this.env = env
        input.open(env)
        super.open(env)
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
        super.close()
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun conditionIsTrue(record: Record, expr: Operator.Expr): Boolean {
        val condition = expr.eval(env.nest(record))
        return condition is BoolValue && condition.value == true
    }
}
