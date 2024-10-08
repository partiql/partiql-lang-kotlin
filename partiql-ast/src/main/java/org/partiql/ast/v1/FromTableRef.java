package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

/**
 * TODO docs, equals, hashcode
 */
public abstract class FromTableRef extends AstNode {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof FromExpr) {
            return visitor.visitFromExpr((FromExpr) this, ctx);
        } else if (this instanceof FromJoin) {
            return visitor.visitFromJoin((FromJoin) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
