package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.Identifier
import org.partiql.ast.v1.SetQuantifier

/**
 * TODO docs, equals, hashcode
 */
public class ExprCall(
    @JvmField
    public var function: Identifier,
    @JvmField
    public var args: List<Expr>,
    @JvmField
    public var setq: SetQuantifier?,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(function)
        kids.addAll(args)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprCall(this, ctx)
}
