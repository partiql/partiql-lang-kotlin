package org.partiql.ast.v1

import org.partiql.ast.v1.expr.Expr

/**
 * TODO docs, equals, hashcode
 */
public class SelectValue(
    @JvmField
    public var `constructor`: Expr,
    @JvmField
    public var setq: SetQuantifier?,
) : Select() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(constructor)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitSelectValue(this, ctx)
}
