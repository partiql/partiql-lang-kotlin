package org.partiql.planner.errors

/**
 * TODO
 *
 */
fun interface PartiQLPlannerErrorHandler {

    fun handle(error: PartiQLPlannerError)
}

/**
 * TODO
 *
 */
internal class ErrorCollector : PartiQLPlannerErrorHandler {

    private val errors = mutableListOf<PartiQLPlannerError>()

    val hasErrors: Boolean
        get() = errors.any { it.severity == PartiQLPlannerError.Severity.ERROR }

    val hasWarnings: Boolean
        get() = errors.any { it.severity == PartiQLPlannerError.Severity.WARNING }

    override fun handle(error: PartiQLPlannerError) {
        errors.add(error)
    }
}
