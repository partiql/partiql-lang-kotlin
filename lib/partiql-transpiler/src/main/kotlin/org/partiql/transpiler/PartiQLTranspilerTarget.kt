package org.partiql.transpiler

import org.partiql.plan.PartiQLPlan

/**
 * A target determines the behavior of each stage of the transpilation.
 */
public interface PartiQLTranspilerTarget<T> {

    /**
     * Target identifier, useful for debugging information.
     */
    public val target: String

    /**
     * Target version, useful for distinguishing slight variations of targets.
     */
    public val version: String

    /**
     * Implement [PartiQLPlan] to desired transpiler output here.
     *
     * @param plan
     * @param onProblem
     * @return
     */
    public fun retarget(plan: PartiQLPlan, onProblem: ProblemCallback): T
}
