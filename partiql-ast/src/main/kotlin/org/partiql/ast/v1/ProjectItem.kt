package org.partiql.ast.v1

import org.partiql.ast.v1.expr.Expr

/**
 * TODO docs, equals, hashcode
 */
public abstract class ProjectItem : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is All -> visitor.visitProjectItemAll(this, ctx)
        is Expression -> visitor.visitProjectItemExpression(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class All(
        @JvmField
        public var expr: Expr,
    ) : ProjectItem() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitProjectItemAll(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Expression(
        @JvmField
        public var expr: Expr,
        @JvmField
        public var asAlias: Identifier.Symbol?,
    ) : ProjectItem() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            asAlias?.let { kids.add(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitProjectItemExpression(this, ctx)
    }
}
