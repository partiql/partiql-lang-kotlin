package org.partiql.ast.v1.expr

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.Sort

/**
 * TODO docs, equals, hashcode
 */
public class ExprWindow(
    @JvmField
    public var function: Function,
    @JvmField
    public var expression: Expr,
    @JvmField
    public var offset: Expr?,
    @JvmField
    public var default: Expr?,
    @JvmField
    public var over: Over,
) : Expr() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(expression)
        offset?.let { kids.add(it) }
        default?.let { kids.add(it) }
        kids.add(over)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExprWindow(this, ctx)

    /**
     * TODO docs, equals, hashcode
     */
    public enum class Function {
        LAG,
        LEAD,
        OTHER,
    }

    /**
     * TODO docs, equals, hashcode
     */
    public class Over(
        @JvmField
        public var partitions: List<Expr>?,
        @JvmField
        public var sorts: List<Sort>?,
    ) : AstNode() {
        public override fun children(): Collection<AstNode> {
            val kids = mutableListOf<AstNode?>()
            partitions?.let { kids.addAll(it) }
            sorts?.let { kids.addAll(it) }
            return kids.filterNotNull()
        }

        public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
            visitor.visitExprWindowOver(this, ctx)
    }
}
