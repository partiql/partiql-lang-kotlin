package org.partiql.eval.internal.vm

import org.partiql.eval.Environment
import org.partiql.eval.ExecutionPlan
import org.partiql.eval.Mode
import org.partiql.eval.PartiQLVM
import org.partiql.eval.internal.compiler.OperatorCompiler
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.Context
import org.partiql.spi.catalog.ExecutionCatalog
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.value.Datum

/**
 * Standard implementation of [PartiQLVM].
 *
 * Each call to [execute] builds a fresh operator tree from the plan — no shared mutable state between executions.
 */
internal class StandardVM : PartiQLVM {

    override fun execute(plan: ExecutionPlan, mode: Mode, catalogs: Array<ExecutionCatalog>): Datum {
        return execute(plan, mode, catalogs, Context.standard())
    }

    override fun execute(plan: ExecutionPlan, mode: Mode, catalogs: Array<ExecutionCatalog>, ctx: Context): Datum {
        try {
            val impl = plan.getImpl()
            val compiler = OperatorCompiler(catalogs, mode)
            val root = compiler.compile(impl)
            return root.eval(Environment())
        } catch (e: PRuntimeException) {
            throw e
        } catch (t: Throwable) {
            throw PErrors.internalErrorException(t)
        }
    }
}
