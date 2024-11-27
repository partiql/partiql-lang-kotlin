package org.partiql.plan.rex

import org.partiql.plan.Visitor

/**
 * TODO DOCUMENTATION
 */
public interface RexArray : Rex {

    public fun getValues(): Collection<Rex>

   

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) { = visitor.visitArray(this, ctx)
}

/**
 * Default [RexArray] operator for extension.
 */
internal class RexArrayImpl(values: Collection<Rex>, type: RexType) : RexArray {

    // DO NOT USE FINAL
    private var _values = values
    private var _type = type

    override fun getValues(): Collection<Rex> = _values

   

    override fun getType(): RexType = _type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexArray) return false
        if (_values != other.getValues()) return false
        return true
    }

    override fun hashCode(): Int = _values.hashCode()
}
