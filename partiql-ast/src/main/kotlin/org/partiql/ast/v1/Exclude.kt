package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class Exclude(
    @JvmField
    public var excludePaths: List<ExcludePath>,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(excludePaths)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExclude(this, ctx)
}
