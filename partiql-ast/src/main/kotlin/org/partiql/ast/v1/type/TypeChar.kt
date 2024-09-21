package org.partiql.ast.v1.type

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public class TypeChar(
    @JvmField
    public var length: Int?,
) : Type() {
    public override fun children(): Collection<AstNode> = emptyList()

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitTypeChar(this, ctx)
}
