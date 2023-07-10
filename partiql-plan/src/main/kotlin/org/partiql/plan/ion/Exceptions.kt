package org.partiql.plan.ion

/**
 * Base PartiQLPlanWriter exception.
 */
public open class PlanWriterException : Exception()

/**
 * PartiQLPlan Ion reading exception.
 *
 * @property message
 * @property cause
 */
public class IllegalPlanException(
    override val message: String?,
    override val cause: Throwable? = null,
) : PlanWriterException()

/**
 * PartiQLPlan Ion reading exception.
 *
 * @property message
 * @property cause
 */
public class MalformedPlanException(
    override val message: String?,
    override val cause: Throwable? = null,
) : PlanWriterException()
