package org.partiql.ast.v1.expr;

import org.jetbrains.annotations.NotNull;
import org.partiql.ast.v1.AstNode;
import org.partiql.ast.v1.AstVisitor;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
public class ExprSessionAttribute extends Expr {
    @NotNull
    public Attribute attribute;

    public ExprSessionAttribute(@NotNull Attribute attribute) {
        this.attribute = attribute;
    }

    @Override
    @NotNull
    public Collection<AstNode> children() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprSessionAttribute(this, ctx);
    }

    /**
     * TODO docs, equals, hashcode
     */
    public enum Attribute {
        CURRENT_USER,
        CURRENT_DATE,
        OTHER
    }
}
