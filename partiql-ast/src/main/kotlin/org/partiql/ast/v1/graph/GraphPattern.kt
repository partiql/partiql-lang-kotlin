package org.partiql.ast.v1.graph

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor
import org.partiql.ast.v1.expr.Expr

/**
 * TODO docs, equals, hashcode
 */
public class GraphPattern(
    @JvmField
    public var restrictor: GraphRestrictor?,
    @JvmField
    public var prefilter: Expr?,
    @JvmField
    public var variable: String?,
    @JvmField
    public var quantifier: GraphQuantifier?,
    @JvmField
    public var parts: List<GraphPart>,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        prefilter?.let { kids.add(it) }
        quantifier?.let { kids.add(it) }
        kids.addAll(parts)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitGraphMatchPattern(this, ctx)
}
