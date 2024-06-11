package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.types.PType

internal class ExprCase(
    private val branches: List<Pair<Operator.Expr, Operator.Expr>>,
    private val default: Operator.Expr
) : Operator.Expr {

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
        return this.type.kind == PType.Kind.BOOL && !this.isNull && this.boolean
    }
}
