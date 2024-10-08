package org.partiql.plan.rel

/**
 * TODO DOCUMENTATION
 */
public interface Rel {

    public fun getType(): RelType

    public fun isOrdered(): Boolean

    public fun getChildren(): Collection<Rel>

    public fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R
}
