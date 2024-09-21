package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public class ExprSessionAttribute(
    @JvmField
    public var attribute: Attribute,
) : Expr() {
    public override fun children(): Collection<AstNode> = emptyList()

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprSessionAttribute(this, ctx)

    /**
     * TODO docs, equals, hashcode
     */
    public enum class Attribute {
        CURRENT_USER,
        CURRENT_DATE,
        OTHER,
    }
}
