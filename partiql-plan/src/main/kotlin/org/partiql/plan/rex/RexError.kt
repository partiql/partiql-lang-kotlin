package org.partiql.plan.rex

/**
 * This represents scenarios in which certain operations are statically known to fail in strict mode but return missing
 * in permissive mode.
 */
public interface RexError : Rex {

    override fun getType(): RexType {
        TODO("Not yet implemented")
    }

    override fun getChildren(): Collection<Rex> = emptyList()

    override fun <R, C> accept(visitor: RexVisitor<R, C>, ctx: C): R = visitor.visitError(this, ctx)
}

internal class RexErrorImpl : RexError
