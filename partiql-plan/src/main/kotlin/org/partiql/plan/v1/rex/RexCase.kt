package org.partiql.plan.v1.rex

/**
 * Representative of the simple CASE-WHEN.
 */
public interface RexCase : Rex {

    public fun getMatch(): Rex

    public fun getBranches(): List<RexCaseBranch>

    public fun getDefault(): Rex

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexCase(this, ctx)
}
