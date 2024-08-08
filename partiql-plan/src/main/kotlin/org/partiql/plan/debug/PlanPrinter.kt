package org.partiql.plan.debug

import org.partiql.plan.PartiQLPlan

/**
 * Basic printer for debugging during early development lifecycle
 *
 * Useful for debugging while the Jackson Poem doesn't handle map serde.
 */
public object PlanPrinter {

    public fun toString(plan: PartiQLPlan): String = buildString { append("DEBUG") }
}
