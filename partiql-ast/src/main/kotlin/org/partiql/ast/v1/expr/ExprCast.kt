package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.type.Type

/**
 * TODO docs, equals, hashcode
 */
public class ExprCast(
    @JvmField
    public var `value`: Expr,
    @JvmField
    public var asType: Type,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(value)
        kids.add(asType)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprCast(this, ctx)
}
