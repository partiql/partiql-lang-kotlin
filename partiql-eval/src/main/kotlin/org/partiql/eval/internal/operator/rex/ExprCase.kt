package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.operator.Expression
import org.partiql.spi.value.Datum
import org.partiql.types.PType

internal class ExprCase(
    private val branches: List<Pair<Expression, Expression>>,
    private val default: Expression
) : Expression {

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
        return this.type.kind == PType.Kind.BOOL && !this.isNull && !this.isMissing && this.boolean
    }
}
