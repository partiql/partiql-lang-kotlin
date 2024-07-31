package org.partiql.plan.v1.rex

/**
 * TODO DOCUMENTATION
 */
interface RexGlobal : Rex {

    fun getCatalog(): String

    /**
     * TODO replace with Catalog Name
     */
    fun getName(): String

    public override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitRexGlobal(this, ctx)
}
