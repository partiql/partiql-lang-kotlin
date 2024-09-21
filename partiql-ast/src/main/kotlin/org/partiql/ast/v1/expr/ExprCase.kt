package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public class ExprCase(
    @JvmField
    public var expr: Expr?,
    @JvmField
    public var branches: List<Branch>,
    @JvmField
    public var default: Expr?,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        expr?.let { kids.add(it) }
        kids.addAll(branches)
        default?.let { kids.add(it) }
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprCase(this, ctx)

    /**
     * TODO docs, equals, hashcode
     */
    public class Branch(
        @JvmField
        public var condition: Expr,
        @JvmField
        public var expr: Expr,
    ) : AstNode() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(condition)
            kids.add(expr)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprCaseBranch(this, ctx)
    }
}
