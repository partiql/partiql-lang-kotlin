package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class From : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is FromValue -> visitor.visitFromValue(this, ctx)
        is FromJoin -> visitor.visitFromJoin(this, ctx)
        else -> throw NotImplementedError()
    }
}
