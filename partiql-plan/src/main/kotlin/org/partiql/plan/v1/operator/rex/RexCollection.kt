package org.partiql.plan.v1.operator.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexCollection : Rex {

    public fun getValues(): List<Rex>

    override fun getOperands(): List<Rex> = getValues()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCollection(this, ctx)
}

/**
 * Default [RexCollection] operator for extension.
 */
internal class RexCollectionImpl(values: List<Rex>) : RexCollection {

    // DO NOT USE FINAL
    private var _values = values

    override fun getValues(): List<Rex> = _values

    override fun getOperands(): List<Rex> = _values

    override fun getType(): PType {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexCollection) return false
        if (_values != other.getValues()) return false
        return true
    }

    override fun hashCode(): Int = _values.hashCode()
}
