package org.partiql.eval

import org.partiql.eval.internal.Compiler
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Symbols
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.PartiQLPlan
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

internal class PartiQLEngineDefault : PartiQLEngine {

    @OptIn(PartiQLValueExperimental::class)
    override fun prepare(plan: PartiQLPlan, session: PartiQLEngine.Session): PartiQLStatement<*> {
        try {
            // 1. Validate all references
            val symbols = Symbols.build(plan, session)
            // 2. Compile with built symbols
            val compiler = Compiler(plan, session, symbols)
            val expression = compiler.compile()
            when (expression) {
                is Operator.Aggregation -> TODO("not possible")
                is Operator.Ddl -> return object : PartiQLStatement.Ddl {
                    override fun execute(): PartiQLValue {
                        return expression.create()
                    }
                }
                is Operator.Expr -> return object : PartiQLStatement.Query {
                    override fun execute(): PartiQLValue {
                        return expression.eval(Environment.empty)
                    }
                }
                is Operator.Relation -> TODO("not possible")
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

            is PartiQLStatement.Ddl -> try {
                val value = statement.execute()
                PartiQLResult.Value(value)
            } catch (ex: Exception) {
                PartiQLResult.Error(ex)
            }
        }
    }
}
