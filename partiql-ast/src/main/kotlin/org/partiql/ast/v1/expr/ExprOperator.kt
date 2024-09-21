package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public class ExprOperator(
    @JvmField
    public var symbol: String,
    @JvmField
    public var lhs: Expr?,
    @JvmField
    public var rhs: Expr,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        lhs?.let { kids.add(it) }
        kids.add(rhs)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprOperator(this, ctx)
}
