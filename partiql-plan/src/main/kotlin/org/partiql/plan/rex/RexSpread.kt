package org.partiql.plan.rex

import org.partiql.plan.Visitor

/**
 * TODO DOCUMENTATION
 */
public interface RexSpread : Rex {

    public fun getArgs(): List<Rex>

    override fun getChildren(): Collection<Rex> = getArgs()

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitSpread(this, ctx)
}

/**
 * Default [RexSpread] operator intended for extension.
 */
internal class RexSpreadImpl(args: List<Rex>, type: RexType) : RexSpread {

    // DO NOT USE FINAL
    private var _args = args
    private var _type = type

    override fun getArgs(): List<Rex> = _args

    override fun getType(): RexType = _type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexSpreadImpl) return false
        if (_args != other._args) return false
        if (_type != other._type) return false
        return true
    }

    override fun hashCode(): Int {
        var result = _args.hashCode()
        result = 31 * result + _type.hashCode()
        return result
    }
}
