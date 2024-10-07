package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

/**
 * TODO docs, equals, hashcode
 */
public abstract class Select extends AstNode {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof SelectStar) {
            return visitor.visitSelectStar((SelectStar) this, ctx);
        } else if (this instanceof SelectList) {
            return visitor.visitSelectList((SelectList) this, ctx);
        } else if (this instanceof SelectPivot) {
            return visitor.visitSelectPivot((SelectPivot) this, ctx);
        } else if (this instanceof SelectValue) {
            return visitor.visitSelectValue((SelectValue) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
