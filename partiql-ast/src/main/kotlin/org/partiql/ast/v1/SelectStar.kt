package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class SelectStar(
    @JvmField
    public var setq: SetQuantifier?,
) : Select() {
    public override fun children(): Collection<AstNode> = emptyList()

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitSelectStar(this, ctx)
}
