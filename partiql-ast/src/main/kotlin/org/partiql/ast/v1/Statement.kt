package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class Statement : AstNode() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is Query -> visitor.visitQuery(this, ctx)
        is DDL -> visitor.visitDDL(this, ctx)
        is Explain -> visitor.visitExplain(this, ctx)
        else -> throw NotImplementedError()
    }
}
