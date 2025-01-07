package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Represents SQL session attributes as an expression. E.g. {@code CURRENT_USER}.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprSessionAttribute extends Expr {
    @NotNull
    private final SessionAttribute sessionAttribute;

    public ExprSessionAttribute(@NotNull SessionAttribute sessionAttribute) {
        this.sessionAttribute = sessionAttribute;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprSessionAttribute(this, ctx);
    }

    @NotNull
    public SessionAttribute getSessionAttribute() {
        return this.sessionAttribute;
    }
}
