package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public class ExprCollection(
    @JvmField
    public var type: Type,
    @JvmField
    public var values: List<Expr>,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(values)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprCollection(this, ctx)

    /**
     * TODO docs, equals, hashcode
     */
    public enum class Type {
        BAG,
        ARRAY,
        VALUES,
        LIST,
        SEXP,
        OTHER,
    }
}
