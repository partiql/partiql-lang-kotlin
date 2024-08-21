package org.partiql.plan.v1.operator.rex

import org.partiql.types.PType

/**
 * TODO DOCUMENTATION
 */
public interface RexCoalesce : Rex {

    public fun getArgs(): List<Rex>

    override fun getOperands(): List<Rex> = getArgs()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitCoalesce(this, ctx)
}

internal class RexCoalesceImpl(args: List<Rex>) : RexCoalesce {

    // DO NOT USE FINAL
    private var _args = args

    override fun getArgs(): List<Rex> = _args

    override fun getOperands(): List<Rex> = _args

    override fun getType(): PType {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RexCoalesce) return false
        if (_args != other.getArgs()) return false
        return true
    }

    override fun hashCode(): Int = _args.hashCode()
}
