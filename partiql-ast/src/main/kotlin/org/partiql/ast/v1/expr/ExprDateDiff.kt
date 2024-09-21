package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.DatetimeField

/**
 * TODO docs, equals, hashcode
 */
public class ExprDateDiff(
    @JvmField
    public var `field`: DatetimeField,
    @JvmField
    public var lhs: Expr,
    @JvmField
    public var rhs: Expr,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(lhs)
        kids.add(rhs)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprDateDiff(this, ctx)
}
