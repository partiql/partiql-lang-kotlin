package org.partiql.plan.v1.rex

import org.partiql.plan.v1.rel.Rel

/**
 * TODO DOCUMENTATION
 *
 * - x IN (<subquery>)
 * - (x,y,z) IN (<subquery>)
 */
public interface RexSubqueryIn : Rex {

    public fun getInput(): Rel

    public fun getValues(): List<Rex>

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexSubqueryIn(this, ctx)
}
