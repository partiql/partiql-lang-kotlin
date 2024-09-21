package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class Explain(
    @JvmField
    public var target: Target,
) : Statement() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(target)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExplain(this, ctx)
}
