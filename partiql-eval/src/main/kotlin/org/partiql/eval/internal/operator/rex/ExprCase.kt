package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class ExprCase(
    private val branches: List<Pair<Operator.Expr, Operator.Expr>>,
    private val default: Operator.Expr
) : Operator.Expr {

    @OptIn(PartiQLValueExperimental::class)
    override fun eval(env: Environment): PartiQLValue {
        branches.forEach { branch ->
            val condition = branch.first.eval(env)
            if (condition.isTrue()) {
                return branch.second.eval(env)
            }
        }
        return default.eval(env)
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun PartiQLValue.isTrue(): Boolean {
        return this is BoolValue && this.value == true
    }
}
