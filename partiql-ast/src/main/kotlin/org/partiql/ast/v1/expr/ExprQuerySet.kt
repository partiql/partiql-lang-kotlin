package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.OrderBy
import org.partiql.ast.v1.QueryBody

/**
 * TODO docs, equals, hashcode
 */
public class ExprQuerySet(
    @JvmField
    public var body: QueryBody,
    @JvmField
    public var orderBy: OrderBy?,
    @JvmField
    public var limit: Expr?,
    @JvmField
    public var offset: Expr?,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(body)
        orderBy?.let { kids.add(it) }
        limit?.let { kids.add(it) }
        offset?.let { kids.add(it) }
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprQuerySet(this, ctx)
}
