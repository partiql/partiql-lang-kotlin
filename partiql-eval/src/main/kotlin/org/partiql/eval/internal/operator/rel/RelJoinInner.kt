package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import java.util.Stack

internal class RelJoinInner(
    override val lhs: Operator.Relation,
    override val rhs: Operator.Relation,
    override val condition: Operator.Expr,
    override val scopes: Stack<Record>
) : RelJoinNestedLoop() {
    override fun join(condition: Boolean, lhs: Record, rhs: Record): Record? {
        return when (condition) {
            true -> lhs + rhs
            false -> null
        }
    }
}
