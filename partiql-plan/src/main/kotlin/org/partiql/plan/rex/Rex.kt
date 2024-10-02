package org.partiql.plan.rex

/**
 * TODO DOCUMENTATION
 */
public interface Rex {

    public fun getType(): RexType

    public fun getChildren(): Collection<Rex>

    public fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R
}
