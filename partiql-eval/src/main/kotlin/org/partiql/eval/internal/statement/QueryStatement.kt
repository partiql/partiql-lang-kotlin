package org.partiql.eval.internal.statement

import org.partiql.eval.PartiQLResult
import org.partiql.eval.PartiQLStatement
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.operator.Operator
import org.partiql.planner.catalog.Session

internal class QueryStatement(root: Operator.Expr) : PartiQLStatement {

    // DO NOT USE FINAL
    private var _root = root

    override fun execute(session: Session): PartiQLResult {
        val datum = _root.eval(Environment.empty)
        val value = PartiQLResult.Value(datum)
        return value
    }
}
