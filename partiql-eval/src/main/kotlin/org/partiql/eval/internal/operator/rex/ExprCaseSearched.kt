package org.partiql.eval.internal.operator.rex

import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum

/**
 * Implementation of the <searched case> expression.
 */
internal class ExprCaseSearched(branches: List<ExprCaseBranch>, default: Operator.Expr?) : Operator.Expr {

    // DO NOT USE FINAL
    private var _branches = branches
    private var _default = default

    override fun eval(): Datum {
        // search
        for (branch in _branches) {
            val result = branch.eval()
            if (result != null) {
                return result
            }
        }
        // default
        return _default?.eval() ?: Datum.nullValue()
    }
}
