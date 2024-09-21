package org.partiql.ast.v1

import org.partiql.ast.v1.type.Type

/**
 * TODO docs, equals, hashcode
 */
public class Column(
    @JvmField
    public var name: String,
    @JvmField
    public var type: Type,
    @JvmField
    public var constraints: List<Constraint>,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(type)
        kids.addAll(constraints)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitColumn(this, ctx)
}
