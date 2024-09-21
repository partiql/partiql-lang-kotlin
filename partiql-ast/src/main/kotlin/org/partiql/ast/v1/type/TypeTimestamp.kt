package org.partiql.ast.v1.type

import org.partiql.ast.v1.AstNode
import org.partiql.ast.v1.AstVisitor

/**
 * TODO docs, equals, hashcode
 */
public class TypeTimestamp(
    @JvmField
    public var precision: Int?,
) : Type() {
    public override fun children(): Collection<AstNode> = emptyList()

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitTypeTimestamp(this, ctx)
}
