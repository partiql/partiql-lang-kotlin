package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

/**
 * TODO docs, equals, hashcode
 */
public abstract class DDL extends Statement {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof CreateTable) {
            return visitor.visitCreateTable((CreateTable) this, ctx);
        } else if (this instanceof CreateIndex) {
            return visitor.visitCreateIndex((CreateIndex) this, ctx);
        } else if (this instanceof DropTable) {
            return visitor.visitDropTable((DropTable) this, ctx);
        } else if (this instanceof DropIndex) {
            return visitor.visitDropIndex((DropIndex) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
