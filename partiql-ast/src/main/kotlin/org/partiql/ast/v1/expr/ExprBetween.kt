package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public class ExprBetween(
    @JvmField
    public var `value`: Expr,
    @JvmField
    public var from: Expr,
    @JvmField
    public var to: Expr,
    @JvmField
    public var not: Boolean?,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(value)
        kids.add(from)
        kids.add(to)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprBetween(this, ctx)
}
