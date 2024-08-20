package org.partiql.plan.v1

public interface PartiQLPlan {

    public fun getStatement(): Statement
}

/**
 * TODO INTERNALIZE ME
 * @property statement
 */
public class PartiQLPlanImpl(
    private val statement: Statement
) : PartiQLPlan {
    override fun getStatement(): Statement = statement
}
