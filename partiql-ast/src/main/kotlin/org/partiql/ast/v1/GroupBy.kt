package org.partiql.ast.v1

import org.partiql.ast.v1.expr.Expr

/**
 * TODO docs, equals, hashcode
 */
public class GroupBy(
    @JvmField
    public var strategy: Strategy,
    @JvmField
    public var keys: List<Key>,
    @JvmField
    public var asAlias: Identifier.Symbol?,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(keys)
        asAlias?.let { kids.add(it) }
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitGroupBy(this, ctx)

    /**
     * TODO docs, equals, hashcode
     */
    public enum class Strategy {
        FULL,
        PARTIAL,
        OTHER,
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Key(
        @JvmField
        public var expr: Expr,
        @JvmField
        public var asAlias: Identifier.Symbol?,
    ) : AstNode() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            asAlias?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitGroupByKey(this, ctx)
    }
}
