package org.partiql.plan.v1.rex

import org.partiql.plan.v1.rel.Rel

/**
 * TODO DOCUMENTATION
 *
 * - x IN (<subquery>)
 * - (x,y,z) IN (<subquery>)
 */
interface RexSubqueryIn {

    fun getInput(): Rel

    fun getValues(): List<Rex>
}
