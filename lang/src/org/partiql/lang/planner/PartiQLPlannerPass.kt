package org.partiql.lang.planner

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ProblemHandler
import org.partiql.pig.runtime.DomainNode

/**
 * As of now, we cannot place lower bounds because PIG permuted domains are in Kotlin are unrelated.
 */
fun interface PartiQLPlannerPass<T : DomainNode> {
    fun apply(plan: T, problemHandler: ProblemHandler): T

    fun interface Physical : PartiQLPlannerPass<PartiqlPhysical.Plan>
}
