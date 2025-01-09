package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Represents a parameter reference. E.g. {@code ?}.
 * <p>
 * Note: this is an experimental API. Class's fields and behavior may change in a subsequent release.
 * </p>
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprParameter extends Expr {
    private final int index;

    public ExprParameter(int index) {
        this.index = index;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprParameter(this, ctx);
    }

    public int getIndex() {
        return this.index;
    }
}
