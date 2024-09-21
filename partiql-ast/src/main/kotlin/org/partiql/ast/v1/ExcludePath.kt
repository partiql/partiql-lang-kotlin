package org.partiql.ast.v1

import org.partiql.ast.v1.expr.ExprVar

/**
 * TODO docs, equals, hashcode
 */
public class ExcludePath(
    @JvmField
    public var root: ExprVar,
    @JvmField
    public var excludeSteps: List<ExcludeStep>,
) : AstNode() {
    public override fun children(): Collection<AstNode> {
        val kids = mutableListOf<AstNode?>()
        kids.add(root)
        kids.addAll(excludeSteps)
        return kids.filterNotNull()
    }

    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R =
        visitor.visitExcludePath(this, ctx)
}
