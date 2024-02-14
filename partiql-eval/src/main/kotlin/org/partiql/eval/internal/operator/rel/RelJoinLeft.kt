package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator

internal class RelJoinLeft(
    override val lhs: Operator.Relation,
    override val rhs: Operator.Relation,
    override val condition: Operator.Expr,
    override val env: Environment
) : RelJoinNestedLoop() {

    override fun join(condition: Boolean, lhs: Record, rhs: Record): Record {
        if (condition.not()) {
            rhs.padNull()
        }
        return lhs + rhs
    }
}
