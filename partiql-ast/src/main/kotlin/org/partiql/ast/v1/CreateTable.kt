package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class CreateTable(
    @JvmField
    public var name: Identifier,
    @JvmField
    public var definition: TableDefinition?,
) : DDL() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(name)
        definition?.let { kids.add(it) }
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitCreateTable(this, ctx)
}
