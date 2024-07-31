package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
interface RexCollection : Rex {

    public fun getValues(): List<Rex>

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexCollection(this, ctx)
}
