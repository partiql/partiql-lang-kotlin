package org.partiql.ast.v1;

import org.jetbrains.annotations.NotNull;

/**
 * TODO docs, equals, hashcode
 */
public abstract class ConstraintType extends AstNode {
    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        if (this instanceof ConstraintNullable) {
            return visitor.visitConstraintNullable((ConstraintNullable) this, ctx);
        } else if (this instanceof ConstraintNotNull) {
            return visitor.visitConstraintNotNull((ConstraintNotNull) this, ctx);
        } else if (this instanceof ConstraintCheck) {
            return visitor.visitConstraintCheck((ConstraintCheck) this, ctx);
        } else {
            throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
