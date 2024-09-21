package org.partiql.ast.v1

/**
 * TODO docs, equals, hashcode
 */
public abstract class DDL : Statement() {
    public override fun <R, C> accept(visitor: AstVisitor<R, C>, ctx: C): R = when (this) {
        is CreateTable -> visitor.visitCreateTable(this, ctx)
        is CreateIndex -> visitor.visitCreateIndex(this, ctx)
        is DropTable -> visitor.visitDropTable(this, ctx)
        is DropIndex -> visitor.visitDropIndex(this, ctx)
        else -> throw NotImplementedError()
    }
}
