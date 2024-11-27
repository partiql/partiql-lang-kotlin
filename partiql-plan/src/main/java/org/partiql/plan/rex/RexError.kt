package org.partiql.plan.rex

/**
 * This represents scenarios in which certain operations are statically known to fail in strict mode but return missing
 * in permissive mode.
 */
import org.partiql.plan.Visitor

public interface RexError : Rex {

    override fun getType(): RexType = RexType.dynamic()

    override fun getChildren(): Collection<Rex> = emptyList()

    override fun <R, C> accept(visitor: Visitor<R, C>, ctx: C): R = visitor.visitError(this, ctx)
}

internal class RexErrorImpl : RexError
