package org.partiql.transpiler

import org.partiql.plan.PartiQLPlan
import org.partiql.types.StaticType

/**
 * Result of retargeting.
 *
 * @param T
 * @property schema
 * @property value
 */
public abstract class TpOutput<T>(
    public val schema: StaticType,
    public val value: T,
) {

    abstract override fun toString(): String

    abstract fun toDebugString(): String
}

/**
 * A target determines the behavior of each stage of the transpilation.
 */
public interface TpTarget<T> {

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
    public fun retarget(plan: PartiQLPlan, onProblem: ProblemCallback): TpOutput<T>
}
