package org.partiql.ast.v1

import org.partiql.ast.v1.expr.Expr

/**
 * TODO docs, equals, hashcode
 */
public class FromJoin(
    @JvmField
    public var lhs: From,
    @JvmField
    public var rhs: From,
    @JvmField
    public var type: Type?,
    @JvmField
    public var condition: Expr?,
) : From() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(lhs)
        kids.add(rhs)
        condition?.let { kids.add(it) }
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitFromJoin(this, ctx)

    /**
     * TODO docs, equals, hashcode
     */
    public enum class Type {
        INNER,
        LEFT,
        LEFT_OUTER,
        RIGHT,
        RIGHT_OUTER,
        FULL,
        FULL_OUTER,
        CROSS,
        COMMA,
        OTHER,
    }
}
