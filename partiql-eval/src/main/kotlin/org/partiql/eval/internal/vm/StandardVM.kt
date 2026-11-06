package org.partiql.eval.internal.vm

import org.partiql.eval.Environment
import org.partiql.eval.ExecutionPlan
import org.partiql.eval.PartiQLVM
import org.partiql.eval.internal.compiler.OperatorCompiler
import org.partiql.eval.internal.compiler.QueryDate
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.Context
import org.partiql.spi.catalog.ExecutionCatalog
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.value.Datum
import java.time.LocalDate

/**
 * Standard implementation of [PartiQLVM].
 *
 * Each call to [execute] builds a fresh operator tree from the plan — no shared mutable state between executions.
 * `CURRENT_DATE` is pinned to [clock] once at the start of [execute].
 */
internal class StandardVM @JvmOverloads constructor(
    private val clock: () -> LocalDate = { LocalDate.now() },
) : PartiQLVM {

    override fun execute(plan: ExecutionPlan, catalogs: Array<ExecutionCatalog>): Datum {
        return execute(plan, catalogs, Context.standard())
    }

    override fun execute(plan: ExecutionPlan, catalogs: Array<ExecutionCatalog>, ctx: Context): Datum {
        try {
            val impl = plan.impl
            val queryDate = QueryDate(clock)
            queryDate.begin()
            val compiler = OperatorCompiler(catalogs, impl.mode, queryDate)
            val root = compiler.compile(impl)
            return root.eval(Environment())
        } catch (e: PRuntimeException) {
            throw e
        } catch (t: Throwable) {
            throw PErrors.internalErrorException(t)
        }
    }
}
