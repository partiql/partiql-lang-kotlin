package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum

/**
 * Returns true IFF value is the MISSING value.
 */
internal class ExprIsMissing(value: Operator.Expr) : Operator.Expr {

    /**
     * Left-hand-side of IS MISSING catches typing errors.
     */
    private val inner = ExprPermissive(value)

    override fun eval(env: Environment): Datum {
        val v = inner.eval(env)
        return Datum.boolValue(v.isMissing)
    }
}
