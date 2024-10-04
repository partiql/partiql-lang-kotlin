package org.partiql.plan.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexSpread : Rex {

    public fun getArgs(): List<Rex>

    override fun getChildren(): Collection<Rex> = getArgs()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitSpread(this, ctx)
}

/**
 * Default [RexSpread] operator intended for extension.
 */
internal class RexSpreadImpl(args: List<Rex>) : RexSpread {

    // DO NOT USE FINAL
    private var _args = args
    private var _type = RexType.of(PType.struct())

    override fun getArgs(): List<Rex> = _args

    override fun getType(): RexType = _type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexSpread) return false
        if (_args != other.getArgs()) return false
        return true
    }

    override fun hashCode(): Int {
        return _args.hashCode()
    }
}
