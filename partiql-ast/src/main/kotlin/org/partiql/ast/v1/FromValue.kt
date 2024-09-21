package org.partiql.ast.v1

import org.partiql.ast.v1.expr.Expr

/**
 * TODO docs, equals, hashcode
 */
public class FromValue(
    @JvmField
    public var expr: Expr,
    @JvmField
    public var type: Type,
    @JvmField
    public var asAlias: Identifier.Symbol?,
    @JvmField
    public var atAlias: Identifier.Symbol?,
    @JvmField
    public var byAlias: Identifier.Symbol?,
) : From() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(expr)
        asAlias?.let { kids.add(it) }
        atAlias?.let { kids.add(it) }
        byAlias?.let { kids.add(it) }
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitFromValue(this, ctx)

    /**
     * TODO docs, equals, hashcode
     */
    public enum class Type {
        SCAN,
        UNPIVOT,
        OTHER,
    }
}
