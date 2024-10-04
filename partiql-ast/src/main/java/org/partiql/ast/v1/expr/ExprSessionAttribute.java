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
    public SessionAttribute sessionAttribute;

    public ExprSessionAttribute(@NotNull SessionAttribute sessionAttribute) {
        this.sessionAttribute = sessionAttribute;
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
}
