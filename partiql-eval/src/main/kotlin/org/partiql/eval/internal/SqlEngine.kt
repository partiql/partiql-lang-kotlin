package org.partiql.eval.internal

import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLStatement
import org.partiql.eval.internal.statement.QueryStatement
import org.partiql.plan.v1.PartiQLPlan
import org.partiql.plan.v1.Statement
import org.partiql.planner.catalog.Session

internal class SqlEngine : PartiQLEngine {

    override fun prepare(plan: PartiQLPlan, mode: PartiQLEngine.Mode, session: Session): PartiQLStatement {
        try {
            val statement = plan.getStatement()
            if (statement !is Statement.Query) {
                throw IllegalArgumentException("Only query statements are supported")
            }
            val compiler = SqlCompiler(mode, session)
            val root = compiler.compile(statement.getRoot(), Unit)
            return QueryStatement(root)
        } catch (ex: Exception) {
            // TODO wrap in some PartiQL Exception
            throw ex
        }
    }
}
