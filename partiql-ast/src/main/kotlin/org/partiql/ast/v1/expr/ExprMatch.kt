package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.graph.GraphMatch

/**
 * TODO docs, equals, hashcode
 */
public class ExprMatch(
    @JvmField
    public var expr: Expr,
    @JvmField
    public var pattern: GraphMatch,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(expr)
        kids.add(pattern)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprMatch(this, ctx)
}
