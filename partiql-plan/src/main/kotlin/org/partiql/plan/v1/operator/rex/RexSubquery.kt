package org.partiql.plan.v1.operator.rex

import org.partiql.plan.v1.operator.rel.Rel
import org.partiql.types.PType

/**
 * Scalar subquery coercion.
 */
public interface RexSubquery : Rex {

    public fun getRel(): Rel

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSubquery(this, ctx)
}

/**
 * Implementation of scalar subquery coercion.
 */
internal class RexSubqueryImpl(rel: Rel) : RexSubquery {

    // DO NOT USE FINAL
    private var _rel = rel

    override fun getRel(): Rel = _rel

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
