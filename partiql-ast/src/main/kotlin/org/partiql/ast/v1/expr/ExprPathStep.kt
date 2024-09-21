package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.Identifier

/**
 * TODO docs, equals, hashcode
 */
public abstract class ExprPathStep : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Symbol -> visitor.visitExprPathStepSymbol(this, ctx)
        is Index -> visitor.visitExprPathStepIndex(this, ctx)
        is Wildcard -> visitor.visitExprPathStepWildcard(this, ctx)
        is Unpivot -> visitor.visitExprPathStepUnpivot(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Symbol(
        @JvmField
        public var symbol: Identifier.Symbol,
    ) : ExprPathStep() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(symbol)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprPathStepSymbol(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Index(
        @JvmField
        public var key: Expr,
    ) : ExprPathStep() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(key)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprPathStepIndex(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Wildcard : ExprPathStep() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprPathStepWildcard(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Unpivot : ExprPathStep() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprPathStepUnpivot(this, ctx)
    }
}
