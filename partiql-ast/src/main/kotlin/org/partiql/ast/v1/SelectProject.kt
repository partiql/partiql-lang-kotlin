package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class SelectProject(
    @JvmField
    public var items: List<ProjectItem>,
    @JvmField
    public var setq: SetQuantifier?,
) : Select() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.addAll(items)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitSelectProject(this, ctx)
}
