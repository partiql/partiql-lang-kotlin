package org.partiql.ast.v1

import org.partiql.ast.v1.expr.Expr

/**
 * TODO docs, equals, hashcode
 */
public abstract class ConstraintBody : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Nullable -> visitor.visitConstraintBodyNullable(this, ctx)
        is NotNull -> visitor.visitConstraintBodyNotNull(this, ctx)
        is Check -> visitor.visitConstraintBodyCheck(this, ctx)
        else -> throw NotImplementedError()
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object Nullable : ConstraintBody() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitConstraintBodyNullable(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public object NotNull : ConstraintBody() {
        public override fun children(): Collection<AstNode> = emptyList()

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitConstraintBodyNotNull(this, ctx)
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Check(
        @JvmField
        public var expr: Expr,
    ) : ConstraintBody() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            kids.add(expr)
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitConstraintBodyCheck(this, ctx)
    }
}
