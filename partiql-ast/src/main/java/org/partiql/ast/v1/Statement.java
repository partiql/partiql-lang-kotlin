package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

/**
 * TODO docs, equals, hashcode
 */
public abstract class Statement extends AstNode {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof Query) {
            return visitor.visitQuery((Query) this, ctx);
        } else if (this instanceof DDL) {
            return visitor.visitDDL((DDL) this, ctx);
        } else if (this instanceof Explain) {
            return visitor.visitExplain((Explain) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
