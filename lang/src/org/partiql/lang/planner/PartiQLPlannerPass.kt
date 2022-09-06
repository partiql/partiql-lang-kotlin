package org.partiql.lang.planner

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ProblemHandler
import org.partiql.pig.runtime.DomainNode

/**
 * As of now, we cannot place lower bounds because PIG permuted domains in Kotlin are unrelated.
 *
 * TODO lower the upper bound to `Plan` after https://github.com/partiql/partiql-ir-generator/issues/65
 */
fun interface PartiQLPlannerPass<T : DomainNode> {
    fun apply(plan: T, problemHandler: ProblemHandler): T

    fun interface Physical : PartiQLPlannerPass<PartiqlPhysical.Plan>
}
