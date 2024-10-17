package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.operator.Expression
import org.partiql.spi.value.Datum

/**
 * Implementation of the <searched case> expression.
 */
internal class ExprCaseSearched(branches: List<ExprCaseBranch>, default: Expression?) :
    Expression {

    // DO NOT USE FINAL
    private var _branches = branches
    private var _default = default

    override fun eval(env: Environment): Datum {
        // search
        for (branch in _branches) {
            val result = branch.eval(env)
            if (result != null) {
                return result
            }
        }
        // default
        return _default?.eval(env) ?: Datum.nullValue()
    }
}
