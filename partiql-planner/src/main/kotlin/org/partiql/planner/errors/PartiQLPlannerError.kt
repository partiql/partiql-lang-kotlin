package org.partiql.planner.errors

/**
 *
 *
 * @property severity
 * @property message
 */
data class PartiQLPlannerError(
    val severity: Severity,
    val message: String,
) {

    enum class Severity {
        WARNING,
        ERROR
    }
}
