package org.partiql.ast.expr;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.partiql.ast.AstNode;
import org.partiql.ast.AstVisitor;

import java.util.Collections;
import java.util.List;

/**
 * Represents an error expression node in the AST.
 * <p>
 * This node is used to represent expressions that could not be parsed correctly,
 * such as datetime field keywords (YEAR, MONTH, DAY, HOUR, MINUTE, SECOND) used
 * in expression contexts where they are reserved tokens.
 * <p>
 * The ExprError node preserves the original text for target-specific recovery.
 */
@Builder(builderClassName = "Builder")
@EqualsAndHashCode(callSuper = false)
public final class ExprError extends Expr {

    /**
     * The original text that caused the error.
     */
    @NotNull
    private final String text;

    /**
     * Creates a new ExprError node.
     *
     * @param text the original text that caused the error
     */
    public ExprError(@NotNull String text) {
        this.text = text;
    }

    @Override
    @NotNull
    public List<AstNode> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <R, C> R accept(@NotNull AstVisitor<R, C> visitor, C ctx) {
        return visitor.visitExprError(this, ctx);
    }

    /**
     * Returns the original text that caused the error.
     *
     * @return the original text
     */
    @NotNull
    public String getText() {
        return this.text;
    }
}
