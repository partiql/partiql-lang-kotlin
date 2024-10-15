package org.partiql.eval.internal

import org.partiql.eval.CompilerConfig
import org.partiql.eval.PartiQLEngine
import org.partiql.eval.PartiQLStatement
import org.partiql.eval.internal.operator.rex.ExprMissing
import org.partiql.eval.internal.statement.QueryStatement
import org.partiql.plan.Operation.Query
import org.partiql.plan.Plan
import org.partiql.spi.catalog.Session
import org.partiql.spi.errors.Error
import org.partiql.spi.errors.ErrorListenerException
import org.partiql.types.PType

internal class SqlEngine : PartiQLEngine {

    override fun prepare(plan: Plan, session: Session, config: CompilerConfig): PartiQLStatement {
        try {
            val operation = plan.getOperation()
            if (operation !is Query) {
                throw IllegalArgumentException("Only query statements are supported")
            }
            val compiler = SqlCompiler(config.mode, session)
            val root = compiler.compile(operation.getRex())
            return QueryStatement(root)
        } catch (e: ErrorListenerException) {
            throw e
        } catch (t: Throwable) {
            val error = Error.INTERNAL_ERROR(null, t)
            config.errorListener.error(error)
            return QueryStatement(ExprMissing(PType.unknown()))
        }
    }
}
