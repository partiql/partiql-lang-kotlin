package org.partiql.plan.v1.operator.rex

import org.partiql.plan.v1.operator.rel.Rel
import org.partiql.types.PType

/**
 * Scalar subquery coercion.
 */
public interface RexSubquery : Rex {

    public fun getRel(): Rel

    // TODO REMOVE ME – TEMPORARY UNTIL PLANNER PROPERLY HANDLES SUBQUERIES
    public fun getConstructor(): Rex

    // TODO REMOVE ME – TEMPORARY UNTIL PLANNER PROPERLY HANDLES SUBQUERIES
    public fun asScalar(): Boolean

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSubquery(this, ctx)
}

/**
 * Implementation of scalar subquery coercion.
 */
internal class RexSubqueryImpl(rel: Rel, constructor: Rex, asScalar: Boolean) : RexSubquery {

    // DO NOT USE FINAL
    private var _rel = rel
    private var _constructor = constructor
    private var _asScalar = asScalar

    override fun getRel(): Rel = _rel

    override fun getConstructor(): Rex = _constructor

    override fun asScalar(): Boolean = _asScalar

    override fun getType(): PType {
        TODO("Not yet implemented")
    }

    override fun getChildren(): Collection<Rex> {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexSubquery) return false
        if (_rel != other.getRel()) return false
        return true
    }

    override fun hashCode(): Int {
        return _rel.hashCode()
    }
}
