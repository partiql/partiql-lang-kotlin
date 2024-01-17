package org.partiql.eval

import org.partiql.eval.internal.Compiler
import org.partiql.eval.internal.Record
import org.partiql.plan.PartiQLPlan
import org.partiql.spi.connector.ConnectorBindings
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class PartiQLEngineDefault : PartiQLEngine {

    @OptIn(PartiQLValueExperimental::class)
    override fun prepare(plan: PartiQLPlan, session: PartiQLEngine.Session): PartiQLStatement<*> {
        try {
            val compiler = Compiler(plan, session)
            val expression = compiler.compile()
            return object : PartiQLStatement.Query {
                override fun execute(): PartiQLValue {
                    return expression.eval(Record.empty)
                }
            }
        } catch (ex: Exception) {
            // TODO wrap in some PartiQL Exception
            throw ex
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun execute(statement: PartiQLStatement<*>): PartiQLResult {
        return when (statement) {
            is PartiQLStatement.Query -> try {
                val value = statement.execute()
                PartiQLResult.Value(value)
            } catch (ex: Exception) {
                PartiQLResult.Error(ex)
            }
        }
    }
}
