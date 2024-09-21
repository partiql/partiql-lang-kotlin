package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class DropTable(
    @JvmField
    public var table: Identifier,
) : DDL() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(table)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitDropTable(this, ctx)
}
