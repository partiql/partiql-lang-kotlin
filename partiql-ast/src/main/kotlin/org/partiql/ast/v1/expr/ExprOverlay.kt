package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public class ExprOverlay(
    @JvmField
    public var `value`: Expr,
    @JvmField
    public var overlay: Expr,
    @JvmField
    public var start: Expr,
    @JvmField
    public var length: Expr?,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(value)
        kids.add(overlay)
        kids.add(start)
        length?.let { kids.add(it) }
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprOverlay(this, ctx)
}
