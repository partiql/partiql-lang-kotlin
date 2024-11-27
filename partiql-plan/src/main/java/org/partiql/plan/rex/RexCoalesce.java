package org.partiql.plan.rex

import org.partiql.plan.Visitor

/**
 * TODO DOCUMENTATION
 */
public interface RexCoalesce : Rex {

    public fun getArgs(): List<Rex>

   

    @Override
    default public <R, C> R accept(Visitor<R, C> visitor, C ctx) { = visitor.visitCoalesce(this, ctx)
}

internal class RexCoalesceImpl(args: List<Rex>, type: RexType) : RexCoalesce {

    // DO NOT USE FINAL
    private var _args = args
    private var _type = type

    override fun getArgs(): List<Rex> = _args

   

    override fun getType(): RexType = _type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexCoalesce) return false
        if (_args != other.getArgs()) return false
        return true
    }

    override fun hashCode(): Int = _args.hashCode()
}
