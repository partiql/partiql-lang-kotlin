package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.type.Type

/**
 * TODO docs, equals, hashcode
 */
public class ExprIsType(
    @JvmField
    public var `value`: Expr,
    @JvmField
    public var type: Type,
    @JvmField
    public var not: Boolean?,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(value)
        kids.add(type)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprIsType(this, ctx)
}
