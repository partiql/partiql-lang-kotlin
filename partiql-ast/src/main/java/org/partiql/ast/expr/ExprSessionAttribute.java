package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO docs, equals, hashcode
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public class ExprSessionAttribute extends Expr {
    @NotNull
    public final SessionAttribute sessionAttribute;

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
