package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public class PathLit(
    @JvmField
    public var root: Identifier.Symbol,
    @JvmField
    public var steps: List<PathLitStep>,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(root)
        kids.addAll(steps)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = visitor.visitPathLit(
        this,
        ctx
    )
}
