package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.ValueUtility.isTrue
import org.partiql.eval.operator.Expression
import org.partiql.eval.operator.Relation

internal class RelOpFilter(
    val input: Relation,
    val expr: Expression
) : RelOpPeeking() {

    private lateinit var env: Environment

    override fun openPeeking(env: Environment) {
        this.env = env
        input.open(env)
    }

    override fun peek(): Row? {
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

    private fun conditionIsTrue(row: Row, expr: Expression): Boolean {
        val condition = expr.eval(env.push(row))
        return condition.isTrue()
    }
}
