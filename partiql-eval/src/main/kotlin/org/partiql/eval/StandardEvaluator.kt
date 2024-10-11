package org.partiql.eval

import org.partiql.plan.Plan
import org.partiql.spi.value.Datum

/**
 * A default [PartiQLEvaluator] implementation.
 */
internal class StandardEvaluator(private val compiler: PartiQLCompiler) : PartiQLEvaluator {

    override fun eval(plan: Plan): Datum = compiler.prepare(plan).execute()
}
