package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal class ExprCase(
    private val branches: List<Pair<ExprValue, ExprValue>>,
    private val default: ExprValue
) : ExprValue {

    override fun eval(env: Environment): Datum {
        branches.forEach { branch ->
            val condition = branch.first.eval(env)
            if (condition.isTrue()) {
                return branch.second.eval(env)
            }
        }
        return default.eval(env)
    }

    private fun Datum.isTrue(): Boolean {
        return this.type.code() == PType.BOOL && !this.isNull && !this.isMissing && this.boolean
    }
}
