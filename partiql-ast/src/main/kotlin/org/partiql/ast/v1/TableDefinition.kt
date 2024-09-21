package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class TableDefinition(
    @JvmField
    public var columns: List<Column>,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(columns)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitTableDefinition(this, ctx)
}
