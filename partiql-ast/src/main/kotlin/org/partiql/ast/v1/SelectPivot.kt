package org.partiql.ast.v1

import org.partiql.ast.v1.expr.Expr

/**
 * TODO docs, equals, hashcode
 */
public class SelectPivot(
    @JvmField
    public var key: Expr,
    @JvmField
    public var `value`: Expr,
) : Select() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(key)
        kids.add(value)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitSelectPivot(this, ctx)
}
