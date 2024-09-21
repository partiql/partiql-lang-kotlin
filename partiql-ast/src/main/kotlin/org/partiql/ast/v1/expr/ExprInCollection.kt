package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public class ExprInCollection(
    @JvmField
    public var lhs: Expr,
    @JvmField
    public var rhs: Expr,
    @JvmField
    public var not: Boolean?,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(lhs)
        kids.add(rhs)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprInCollection(this, ctx)
}
