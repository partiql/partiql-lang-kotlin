package org.partiql.eval.internal

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLStatement
import org.partiql.eval.internal.statement.QueryStatement
import org.partiql.plan.Operation.Query
import org.partiql.plan.Plan
import org.partiql.spi.catalog.Session

internal class SqlEngine : PartiQLEngine {

    override fun prepare(plan: Plan, mode: PartiQLEngine.Mode, session: Session): PartiQLStatement {
        try {
            val operation = plan.getOperation()
            if (operation !is Query) {
                throw IllegalArgumentException("Only query statements are supported")
            }
            val compiler = SqlCompiler(mode, session)
            val root = compiler.compile(operation.getRex())
            return QueryStatement(root)
        } catch (ex: Exception) {
            // TODO wrap in some PartiQL Exception
            throw ex
        }
    }
}
