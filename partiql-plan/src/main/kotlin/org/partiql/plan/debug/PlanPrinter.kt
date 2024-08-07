package org.partiql.plan.debug

import org.partiql.plan.Plan

/**
 * Basic printer for debugging during early development lifecycle
 *
 * Useful for debugging while the Jackson Poem doesn't handle map serde.
 */
object PlanPrinter {

    fun toString(plan: Plan): String = buildString { append("DEBUG") }
}
