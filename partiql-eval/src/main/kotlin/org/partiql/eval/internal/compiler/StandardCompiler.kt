package org.partiql.eval.internal.compiler

import org.partiql.eval.Environment
import org.partiql.eval.ExecutionPlan
import org.partiql.eval.Mode
import org.partiql.eval.Statement
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.eval.compiler.Strategy
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.plan.Plan
import org.partiql.spi.Context
import org.partiql.spi.catalog.ExecutionCatalog
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorKind
import org.partiql.spi.errors.PRuntimeException
import org.partiql.spi.value.Datum

/**
 * This class is responsible for producing an executable statement from logical operators.
 */
internal class StandardCompiler(strategies: List<Strategy>) : PartiQLCompiler {

    private val strategies: List<Strategy> = strategies

    internal constructor() : this(emptyList())

    override fun compile(plan: Plan, mode: Mode): ExecutionPlan {
        try {
            PlanValidator.validate(plan)
            val transform = PlanToExecTransform(strategies, mode)
            val impl = transform.transform(plan)
            return ExecutionPlan(impl)
        } catch (e: PRuntimeException) {
            throw e
        } catch (t: Throwable) {
            val error = PError.INTERNAL_ERROR(PErrorKind.COMPILATION(), null, t)
            throw PRuntimeException(error)
        }
    }

    override fun prepare(plan: Plan, mode: Mode, ctx: Context): Statement {
        try {
            val transform = PlanToExecTransform(strategies, mode)
            val impl = transform.transform(plan)
            val compiler = OperatorCompiler(emptyArray(), mode)
            val root = compiler.compile(impl)
            return object : Statement {
                override fun execute(): Datum {
                    return try {
                        root.eval(Environment())
                    } catch (e: PRuntimeException) {
                        throw e
                    } catch (t: Throwable) {
                        throw PErrors.internalErrorException(t)
                    }
                }
            }
        } catch (e: PRuntimeException) {
            throw e
        } catch (t: Throwable) {
            val error = PError.INTERNAL_ERROR(PErrorKind.COMPILATION(), null, t)
            ctx.errorListener.report(error)
            return Statement { Datum.missing() }
        }
    }

    override fun prepare(plan: Plan, mode: Mode, catalogs: Array<ExecutionCatalog>, ctx: Context): Statement {
        try {
            PlanValidator.validate(plan)
            val transform = PlanToExecTransform(strategies, mode)
            val impl = transform.transform(plan)
            return object : Statement {
                override fun execute(): Datum {
                    return try {
                        val compiler = OperatorCompiler(catalogs, mode)
                        compiler.compile(impl).eval(Environment())
                    } catch (e: PRuntimeException) {
                        throw e
                    } catch (t: Throwable) {
                        throw PErrors.internalErrorException(t)
                    }
                }
            }
        } catch (e: PRuntimeException) {
            throw e
        } catch (t: Throwable) {
            val error = PError.INTERNAL_ERROR(PErrorKind.COMPILATION(), null, t)
            ctx.errorListener.report(error)
            return Statement { Datum.missing() }
        }
    }
}
