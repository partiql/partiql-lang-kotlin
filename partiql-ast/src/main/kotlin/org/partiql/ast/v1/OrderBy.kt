package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class OrderBy(
    @JvmField
    public var sorts: List<Sort>,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(sorts)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitOrderBy(this, ctx)
}
