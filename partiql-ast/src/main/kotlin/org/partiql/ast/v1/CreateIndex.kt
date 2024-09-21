package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class CreateIndex(
    @JvmField
    public var index: Identifier?,
    @JvmField
    public var table: Identifier,
    @JvmField
    public var fields: List<PathLit>,
) : DDL() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        index?.let { kids.add(it) }
        kids.add(table)
        kids.addAll(fields)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitCreateIndex(this, ctx)
}
