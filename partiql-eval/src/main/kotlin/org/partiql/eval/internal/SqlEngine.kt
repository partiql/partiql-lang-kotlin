package org.partiql.eval.internal

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLStatement
import org.partiql.eval.internal.statement.QueryStatement
import org.partiql.spi.catalog.Session

internal class SqlEngine : PartiQLEngine {

    override fun prepare(plan: org.partiql.plan.Plan, mode: PartiQLEngine.Mode, session: Session): PartiQLStatement {
        try {
            val statement = plan.getStatement()
            if (statement !is org.partiql.plan.Statement.Query) {
                throw IllegalArgumentException("Only query statements are supported")
            }
            val compiler = SqlCompiler(mode, session)
            val root = compiler.compile(statement.getRoot())
            return QueryStatement(root)
        } catch (ex: Exception) {
            // TODO wrap in some PartiQL Exception
            throw ex
        }
    }
}
