package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public class ExprTrim(
    @JvmField
    public var `value`: Expr,
    @JvmField
    public var chars: Expr?,
    @JvmField
    public var spec: Spec?,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(value)
        chars?.let { kids.add(it) }
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprTrim(this, ctx)

    public enum class Spec {
        LEADING,
        TRAILING,
        BOTH,
        OTHER,
    }
}
