package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.Identifier

/**
 * TODO docs, equals, hashcode
 */
public class ExprVar(
    @JvmField
    public var identifier: Identifier,
    @JvmField
    public var scope: Scope,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(identifier)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprVar(this, ctx)

    /**
     * TODO docs, equals, hashcode
     */
    public enum class Scope {
        DEFAULT,
        LOCAL,
        OTHER,
    }
}
