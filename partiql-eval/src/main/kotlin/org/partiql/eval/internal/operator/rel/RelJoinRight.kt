package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelJoinRight(
    lhs: Operator.Relation,
    rhs: Operator.Relation,
    override val condition: Operator.Expr,
    override val env: Environment
) : RelJoinNestedLoop() {

    override val lhs: Operator.Relation = rhs
    override val rhs: Operator.Relation = lhs

    override fun join(condition: Boolean, lhs: Record, rhs: Record): Record {
        if (condition.not()) {
            lhs.padNull()
        }
        return lhs + rhs
    }
}
