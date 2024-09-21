package org.partiql.ast.v1.graph

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public class GraphQuantifier(
    @JvmField
    public var lower: Long,
    @JvmField
    public var upper: Long?,
) : AstNode() {
    public override fun children(): Collection<AstNode> = emptyList()

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitGraphQuantifier(this, ctx)
}
