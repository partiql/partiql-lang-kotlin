package org.partiql.plan.rex

/**
 * TODO DOCUMENTATION
 */
public interface RexCoalesce : Rex {

    public fun getArgs(): List<Rex>

    override fun getChildren(): Collection<Rex> = getArgs()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCoalesce(this, ctx)
}

internal class RexCoalesceImpl(args: List<Rex>, type: RexType) : RexCoalesce {

    // DO NOT USE FINAL
    private var _args = args
    private var _type = type

    override fun getArgs(): List<Rex> = _args

    override fun getChildren(): Collection<Rex> = _args

    override fun getType(): RexType = _type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexCoalesce) return false
        if (_args != other.getArgs()) return false
        return true
    }

    override fun hashCode(): Int = _args.hashCode()
}
