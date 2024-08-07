package org.partiql.plan.operator.rel

/**
 * Logical `INTERSECT [ALL|DISTINCT]` operator for set (or multiset) intersection.
 */
public interface RelIntersect : Rel {

    public fun isAll(): Boolean

    public fun getLeft(): Rel

    public fun getRight(): Rel

    override fun getInputs(): List<Rel> = listOf(getLeft(), getRight())

    override fun isOrdered(): Boolean = false

    override fun <R, C> accept(visitor: RelVisitor<R, C>, ctx: C): R = visitor.visitIntersect(this, ctx)
}

