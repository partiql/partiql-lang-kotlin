package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class Select : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is SelectStar -> visitor.visitSelectStar(this, ctx)
        is SelectProject -> visitor.visitSelectProject(this, ctx)
        is SelectPivot -> visitor.visitSelectPivot(this, ctx)
        is SelectValue -> visitor.visitSelectValue(this, ctx)
        else -> throw NotImplementedError()
    }
}
