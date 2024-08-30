package org.partiql.plan.v1.operator.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexBag : Rex {

    public fun getValues(): Collection<Rex>

    override fun getChildren(): Collection<Rex> = getValues()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitBag(this, ctx)
}

/**
 * Default [RexBag] operator for extension.
 */
internal class RexBagImpl(values: Collection<Rex>) : RexBag {

    // DO NOT USE FINAL
    private var _values = values

    override fun getValues(): Collection<Rex> = _values

    override fun getChildren(): Collection<Rex> = _values

    override fun getType(): PType = PType.bag()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexBag) return false
        if (_values != other.getValues()) return false
        return true
    }

    override fun hashCode(): Int = _values.hashCode()
}
